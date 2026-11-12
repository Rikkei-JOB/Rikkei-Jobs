package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.ApplicationNotificationOutbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationNotificationOutboxRepository extends JpaRepository<ApplicationNotificationOutbox, UUID> {}
