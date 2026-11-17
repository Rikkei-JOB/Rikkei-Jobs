package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.ApplicationStatusNotification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusNotificationRepository extends JpaRepository<ApplicationStatusNotification, UUID> {}
