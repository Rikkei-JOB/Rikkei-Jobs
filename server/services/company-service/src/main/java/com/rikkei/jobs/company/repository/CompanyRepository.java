package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    boolean existsByTaxCode(String taxCode);

    /** {@code pattern} đã viết thường và thoát ký tự LIKE bằng dấu gạch chéo ngược (xem StaffCompanyServiceImpl). */
    @Query("select c from Company c where (lower(c.name) like :pattern escape '\\' or c.taxCode like :pattern escape '\\')")
    Page<Company> search(@Param("pattern") String pattern, Pageable pageable);

    /** Như {@link #search} nhưng lọc theo trạng thái; tách riêng để không phải bind tham số enum null (PostgreSQL không suy được kiểu). */
    @Query("select c from Company c where (lower(c.name) like :pattern escape '\\' or c.taxCode like :pattern escape '\\') "
            + "and c.status = :status")
    Page<Company> searchByStatus(@Param("pattern") String pattern, @Param("status") CompanyStatus status,
            Pageable pageable);

    /**
     * SELECT … FOR UPDATE: khoá dòng công ty tới hết transaction để hai lời gọi khoá / mở khoá chạy lần lượt
     * (bảng companies không có @Version). Chỉ gọi trong transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> findByIdForUpdate(@Param("id") UUID id);
}
