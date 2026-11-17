package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByTaxCode(String taxCode);

    /** {@code pattern} đã viết thường và thoát ký tự LIKE bằng dấu gạch chéo ngược (xem StaffCompanyServiceImpl). */
    @Query("select c from Company c where (lower(c.name) like :pattern escape '\\' or c.taxCode like :pattern escape '\\') "
            + "and (:status is null or c.status = :status)")
    Page<Company> search(@Param("pattern") String pattern, @Param("status") CompanyStatus status, Pageable pageable);
}
