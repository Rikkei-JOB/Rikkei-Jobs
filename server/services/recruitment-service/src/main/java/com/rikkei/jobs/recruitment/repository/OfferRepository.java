package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.Offer;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    Optional<Offer> findByApplicationId(UUID applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offer o where o.applicationId = :applicationId")
    Optional<Offer> findByApplicationIdForUpdate(@Param("applicationId") UUID applicationId);
}
