package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import com.rikkei.jobs.company.dto.response.StaffCompanyItem;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.service.StaffCompanyService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class StaffCompanyServiceImpl implements StaffCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final IdentityClient identity;

    private static final int MAX_QUERY_LENGTH = 100;
    /** identity chỉ nhận tối đa 100 id mỗi lần lookup. */
    private static final int LOOKUP_BATCH_SIZE = 100;

    @Override
    public PageResponse<StaffCompanyItem> search(String q, CompanyStatus status, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Phân trang không hợp lệ.");
        }
        String keyword = q == null ? "" : q.trim();
        if (keyword.length() > MAX_QUERY_LENGTH) {
            throw new BadRequestException("Từ khoá tìm kiếm tối đa 100 ký tự.");
        }
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));
        PageRequest pageable = PageRequest.of(page - 1, size, sort);
        Page<Company> result = status == null
                ? companyRepository.search(pattern, pageable)
                : companyRepository.searchByStatus(pattern, status, pageable);

        Map<UUID, Long> hrCounts = new HashMap<>();
        if (!result.isEmpty()) {
            List<UUID> ids = result.getContent().stream().map(Company::getId).toList();
            for (Object[] row : companyHrRepository.countByCompanyIds(ids)) {
                hrCounts.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }
        List<StaffCompanyItem> items = result.getContent().stream()
                .map(c -> new StaffCompanyItem(c.getId(), c.getName(), c.getTaxCode(), c.getStatus(),
                        hrCounts.getOrDefault(c.getId(), 0L), 0L, c.getCreatedAt()))
                .toList();
        return PageResponse.of(result, items);
    }

    @Override
    public StaffCompanyDetail detail(UUID id) {
        Company c = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
        List<UUID> hrIds = companyHrRepository.findByCompanyId(id).stream().map(CompanyHr::getUserId).toList();
        // Không có HR thì khỏi gọi identity (lookup yêu cầu 1–100 id); nhiều hơn 100 thì chia lô.
        List<IdentityUserStatus> found = new ArrayList<>();
        for (int from = 0; from < hrIds.size(); from += LOOKUP_BATCH_SIZE) {
            found.addAll(identity.lookupUsers(hrIds.subList(from, Math.min(from + LOOKUP_BATCH_SIZE, hrIds.size()))));
        }
        Set<UUID> foundIds = found.stream().map(IdentityUserStatus::userId).collect(Collectors.toSet());
        List<UUID> missing = hrIds.stream().filter(h -> !foundIds.contains(h)).toList();
        if (!missing.isEmpty()) {
            log.warn("HR {} có trong company_hr của công ty {} nhưng không còn ở identity", missing, id);
        }
        List<StaffHrAccount> hrAccounts = found.stream()
                .sorted(Comparator.comparing(IdentityUserStatus::email,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(StaffHrAccountMapping::from)
                .toList();
        return new StaffCompanyDetail(c.getId(), c.getName(), c.getTaxCode(), c.getIndustry(), c.getSize(),
                c.getAddress(), c.getCity(), c.getWebsite(), c.getLogoUrl(), c.getDescription(),
                c.getStatus(), c.getCreatedAt(), c.getUpdatedAt(), hrAccounts);
    }

    /** Thoát ký tự đại diện của LIKE để "%" và "_" được tìm đúng nghĩa đen. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
