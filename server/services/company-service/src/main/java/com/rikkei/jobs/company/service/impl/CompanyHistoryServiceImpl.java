package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.CompanyActionItem;
import com.rikkei.jobs.company.dto.response.JobRef;
import com.rikkei.jobs.company.dto.response.StaffRef;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.service.CompanyHistoryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Cố ý không @Transactional: lời gọi HTTP sang identity không được nằm trong transaction đang mở. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyHistoryServiceImpl implements CompanyHistoryService {

    /** identity chỉ nhận tối đa 100 id mỗi lần lookup. */
    private static final int LOOKUP_BATCH_SIZE = 100;

    private final CompanyRepository companyRepository;
    private final CompanyActionRepository actionRepository;
    private final JobRepository jobRepository;
    private final IdentityClient identity;

    @Override
    public PageResponse<CompanyActionItem> history(UUID companyId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        if (!companyRepository.existsById(companyId)) {
            throw new ResourceNotFoundException("Không tìm thấy công ty");
        }
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Page<CompanyAction> result = actionRepository.findByCompanyId(companyId, PageRequest.of(page - 1, size, sort));

        List<UUID> jobIds = result.getContent().stream().map(CompanyAction::getTargetId)
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> titles = new HashMap<>();
        if (!jobIds.isEmpty()) {
            for (Job j : jobRepository.findAllById(jobIds)) {
                titles.put(j.getId(), j.getTitle());
            }
        }
        Map<UUID, String> names = staffNames(result.getContent().stream().map(CompanyAction::getStaffId)
                .distinct().toList());

        List<CompanyActionItem> items = result.getContent().stream()
                .map(a -> new CompanyActionItem(a.getId(), a.getAction(), a.getCreatedAt(), a.getReason(),
                        new StaffRef(a.getStaffId(), names.get(a.getStaffId())),
                        a.getTargetId() == null ? null : new JobRef(a.getTargetId(), titles.get(a.getTargetId()))))
                .toList();
        return PageResponse.of(result, items);
    }

    /** Một lần tra cho cả trang (chia lô ≤ 100); lỗi identity thì để trống tên, lịch sử vẫn trả về. */
    private Map<UUID, String> staffNames(List<UUID> staffIds) {
        Map<UUID, String> names = new HashMap<>();
        try {
            for (int from = 0; from < staffIds.size(); from += LOOKUP_BATCH_SIZE) {
                List<IdentityUserStatus> found =
                        identity.lookupUsers(staffIds.subList(from, Math.min(from + LOOKUP_BATCH_SIZE, staffIds.size())));
                for (IdentityUserStatus u : found) {
                    names.put(u.userId(), u.fullName());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Không tra được tên cán bộ ở identity-service: {}", e.getMessage());
            names.clear();
        }
        return names;
    }
}
