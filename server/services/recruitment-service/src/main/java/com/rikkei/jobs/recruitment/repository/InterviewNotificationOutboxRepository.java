package com.rikkei.jobs.recruitment.repository;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InterviewNotificationOutboxRepository extends JpaRepository<InterviewNotificationOutbox,UUID> {}
