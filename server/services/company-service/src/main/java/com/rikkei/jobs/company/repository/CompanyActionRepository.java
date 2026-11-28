package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyActionRepository extends JpaRepository<CompanyAction, UUID> {

    /** Gọi với sort {@code createdAt desc, id desc}. */
    Page<CompanyAction> findByCompanyId(UUID companyId, Pageable pageable);

    Optional<CompanyAction> findFirstByCompanyIdAndActionOrderByCreatedAtDescIdDesc(UUID companyId,
                                                                                    CompanyActionType action);
}
