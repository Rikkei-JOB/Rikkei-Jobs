package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Company;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByTaxCode(String taxCode);
}
