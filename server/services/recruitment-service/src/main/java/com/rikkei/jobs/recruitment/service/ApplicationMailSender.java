package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.entity.Application;
import java.util.UUID;
public interface ApplicationMailSender {
    void sendNewApplication(UUID notificationId, String recipient, Application application);
}
