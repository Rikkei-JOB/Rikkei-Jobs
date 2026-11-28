package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.CompanyHr;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Khoá chính là userId nên {@code findById(userId)} tra công ty của một HR. */
public interface CompanyHrRepository extends JpaRepository<CompanyHr, UUID> {

    List<CompanyHr> findByCompanyId(UUID companyId);

    /** Một truy vấn cho cả trang: mỗi dòng là {companyId, số HR}; công ty không có HR không xuất hiện. */
    @Query("select h.companyId, count(h) from CompanyHr h where h.companyId in :ids group by h.companyId")
    List<Object[]> countByCompanyIds(@Param("ids") Collection<UUID> ids);

    /** Một truy vấn cho HrLockedFilter: khoá riêng và trạng thái công ty của HR; rỗng khi HR chưa gắn công ty. */
    @Query("select new com.rikkei.jobs.company.repository.HrAccessState(h.lockedAt, c.status) "
            + "from CompanyHr h join Company c on c.id = h.companyId where h.userId = :userId")
    Optional<HrAccessState> findAccessState(@Param("userId") UUID userId);
}
