package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.service.CompanyLockService;
import com.rikkei.jobs.company.util.ReasonRules;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ngoại lệ có chủ đích so với thói quen "không gọi HTTP trong transaction" của nhóm (DN-05, commit 34f14fc): trạng
 * thái công ty và cờ khoá theo công ty của HR phải đổi cùng nhau, nên identity được gọi TRONG transaction đang giữ
 * khoá dòng công ty, trước khi commit. identity lỗi → ném lỗi, transaction rollback, không có gì đổi; lô k lỗi thì
 * gọi bù ngay (vẫn giữ khoá dòng) cho các lô trước đã thành công; commit lỗi sau khi identity đã nhận thì gọi bù
 * trong afterCompletion. Gọi bù lỗi chỉ ghi log ERROR (cần sửa tay).
 *
 * <p>Phần ghi chạy trong TransactionTemplate (tương đương @Transactional) để chi tiết trả về được dựng sau khi
 * commit: tra tên cán bộ là lời gọi HTTP không cần giữ khoá dòng.
 */
@Slf4j
@Service
public class CompanyLockServiceImpl implements CompanyLockService {

    /** identity nhận tối đa 100 id mỗi lời gọi company-lock. */
    static final int BATCH_SIZE = 100;

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final CompanyActionRepository companyActionRepository;
    private final IdentityClient identity;
    private final StaffCompanyDetailAssembler detailAssembler;
    private final TransactionTemplate tx;

    public CompanyLockServiceImpl(CompanyRepository companyRepository, CompanyHrRepository companyHrRepository,
                                  CompanyActionRepository companyActionRepository, IdentityClient identity,
                                  StaffCompanyDetailAssembler detailAssembler,
                                  PlatformTransactionManager transactionManager) {
        this.companyRepository = companyRepository;
        this.companyHrRepository = companyHrRepository;
        this.companyActionRepository = companyActionRepository;
        this.identity = identity;
        this.detailAssembler = detailAssembler;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public StaffCompanyDetail lock(UUID staffId, UUID companyId, String reason) {
        return change(staffId, companyId, true, ReasonRules.clean(reason));
    }

    @Override
    public StaffCompanyDetail unlock(UUID staffId, UUID companyId) {
        return change(staffId, companyId, false, null);
    }

    private StaffCompanyDetail change(UUID staffId, UUID companyId, boolean lock, String reason) {
        Outcome outcome = tx.execute(s -> {
            // Khoá dòng ở đầu transaction: hai lời gọi khoá / mở khoá đồng thời chạy lần lượt.
            Company company = companyRepository.findByIdForUpdate(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
            if (lock && company.getStatus() == CompanyStatus.LOCKED) {
                throw new ConflictException("Công ty đã bị khoá");
            }
            if (!lock && company.getStatus() == CompanyStatus.ACTIVE) {
                throw new ConflictException("Công ty không bị khoá");
            }
            company.setStatus(lock ? CompanyStatus.LOCKED : CompanyStatus.ACTIVE);
            Company saved = companyRepository.saveAndFlush(company);
            // flush trước khi gọi identity: lỗi ghi DB lộ ra khi identity chưa nhận gì.
            companyActionRepository.saveAndFlush(new CompanyAction(companyId,
                    lock ? CompanyActionType.LOCK : CompanyActionType.UNLOCK, null, reason, staffId));

            // Mọi HR của công ty, kể cả HR đang bị khoá riêng; sắp theo id để chia lô ổn định.
            List<UUID> hrIds = companyHrRepository.findByCompanyId(companyId).stream()
                    .map(CompanyHr::getUserId).sorted().toList();
            List<IdentityUserStatus> statuses = applyToIdentity(companyId, hrIds, lock);
            if (!hrIds.isEmpty()) {
                compensateOnRollback(companyId, hrIds, lock);
            }
            return new Outcome(saved, hrIds, statuses);
        });
        return detailAssembler.build(outcome.company(), outcome.hrIds(), outcome.statuses());
    }

    /** Gửi từng lô; lô k lỗi thì gọi bù chiều ngược cho các lô trước đó rồi ném lại lỗi (transaction rollback). */
    private List<IdentityUserStatus> applyToIdentity(UUID companyId, List<UUID> hrIds, boolean lock) {
        List<IdentityUserStatus> statuses = new ArrayList<>();
        List<List<UUID>> done = new ArrayList<>();
        for (List<UUID> batch : batches(hrIds)) {
            try {
                statuses.addAll(identity.setCompanyLock(batch, lock));
            } catch (RuntimeException e) {
                compensate(companyId, done, !lock);
                throw e;
            }
            done.add(batch);
        }
        return statuses;
    }

    /** Commit DB lỗi sau khi identity đã nhận đủ: transaction rollback thì đặt lại cờ của mọi HR. */
    private void compensateOnRollback(UUID companyId, List<UUID> hrIds, boolean lock) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.error("Ghi khoá công ty {} rollback sau khi identity đã nhận, gọi bù locked={} cho {} HR",
                            companyId, !lock, hrIds.size());
                    compensate(companyId, batches(hrIds), !lock);
                } else if (status == STATUS_UNKNOWN) {
                    log.error("Không rõ kết quả commit khi đổi khoá công ty {} (locked={}); cần đối chiếu cờ "
                            + "khoá của {} HR với identity", companyId, lock, hrIds.size());
                }
            }
        });
    }

    private void compensate(UUID companyId, List<List<UUID>> batches, boolean locked) {
        for (List<UUID> batch : batches) {
            try {
                identity.setCompanyLock(batch, locked);
            } catch (RuntimeException e) {
                log.error("Gọi bù company-lock locked={} cho HR {} của công ty {} lỗi, cần sửa tay: {}",
                        locked, batch, companyId, e.getMessage());
            }
        }
    }

    private static List<List<UUID>> batches(List<UUID> ids) {
        List<List<UUID>> result = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            result.add(ids.subList(from, Math.min(from + BATCH_SIZE, ids.size())));
        }
        return result;
    }

    private record Outcome(Company company, List<UUID> hrIds, List<IdentityUserStatus> statuses) {
    }
}
