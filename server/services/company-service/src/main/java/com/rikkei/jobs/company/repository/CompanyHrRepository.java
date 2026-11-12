package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.CompanyHr;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Khoá chính là userId nên {@code findById(userId)} tra công ty của một HR. */
public interface CompanyHrRepository extends JpaRepository<CompanyHr, UUID> {
}
