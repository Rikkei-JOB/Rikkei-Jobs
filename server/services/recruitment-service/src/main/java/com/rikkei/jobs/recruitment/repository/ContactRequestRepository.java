package com.rikkei.jobs.recruitment.repository;
import com.rikkei.jobs.recruitment.entity.*;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
public interface ContactRequestRepository extends JpaRepository<ContactRequest,UUID> {
    boolean existsByCompanyIdAndUserIdAndStatus(UUID companyId, UUID userId, ContactRequestStatus status);
    Optional<ContactRequest> findFirstByCompanyIdAndUserIdOrderByCreatedAtDescIdDesc(UUID companyId,UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ContactRequest r where r.id=:id")
    Optional<ContactRequest> findForUpdate(@Param("id") UUID id);
    @Query("select r from ContactRequest r where (:status is null or r.status=:status)")
    Page<ContactRequest> findFiltered(@Param("status") ContactRequestStatus status, Pageable page);
}
