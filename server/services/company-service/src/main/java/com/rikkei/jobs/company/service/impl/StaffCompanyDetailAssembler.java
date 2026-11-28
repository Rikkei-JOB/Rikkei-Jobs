package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.CompanyLockInfo;
import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;
import com.rikkei.jobs.company.dto.response.StaffRef;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dựng StaffCompanyDetail dùng chung cho GET /staff/companies/{id} và khoá / mở khoá công ty. Gọi ngoài transaction:
 * tra tên cán bộ khoá là lời gọi HTTP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class StaffCompanyDetailAssembler {

    private final CompanyActionRepository companyActionRepository;
    private final IdentityClient identity;

    /** {@code found} là trạng thái identity trả về cho {@code hrIds}; HR không còn ở identity bị bỏ qua (log WARN). */
    StaffCompanyDetail build(Company c, List<UUID> hrIds, List<IdentityUserStatus> found) {
        Set<UUID> foundIds = found.stream().map(IdentityUserStatus::userId).collect(Collectors.toSet());
        List<UUID> missing = hrIds.stream().filter(h -> !foundIds.contains(h)).toList();
        if (!missing.isEmpty()) {
            log.warn("HR {} có trong company_hr của công ty {} nhưng không còn ở identity", missing, c.getId());
        }
        List<StaffHrAccount> hrAccounts = found.stream()
                .sorted(Comparator.comparing(IdentityUserStatus::email,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(StaffHrAccountMapping::from)
                .toList();
        return new StaffCompanyDetail(c.getId(), c.getName(), c.getTaxCode(), c.getIndustry(), c.getSize(),
                c.getAddress(), c.getCity(), c.getWebsite(), c.getLogoUrl(), c.getDescription(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt(), hrAccounts, lockInfo(c));
    }

    /** Null khi ACTIVE, hoặc LOCKED mà chưa có dòng LOCK (dữ liệu cũ): không bịa người khoá. */
    private CompanyLockInfo lockInfo(Company c) {
        if (c.getStatus() != CompanyStatus.LOCKED) {
            return null;
        }
        return companyActionRepository.findFirstByCompanyIdAndActionOrderByCreatedAtDescIdDesc(c.getId(),
                        CompanyActionType.LOCK)
                .map(row -> new CompanyLockInfo(row.getCreatedAt(), row.getReason(),
                        new StaffRef(row.getStaffId(), staffName(row.getStaffId()))))
                .orElse(null);
    }

    /** Tên cán bộ chỉ để hiển thị: identity lỗi thì trả null, chi tiết công ty không được hỏng theo. */
    private String staffName(UUID staffId) {
        try {
            return identity.lookupUsers(List.of(staffId)).stream()
                    .filter(u -> staffId.equals(u.userId()))
                    .map(IdentityUserStatus::fullName)
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("Không tra được tên cán bộ {} từ identity-service: {}", staffId, e.getMessage());
            return null;
        }
    }
}
