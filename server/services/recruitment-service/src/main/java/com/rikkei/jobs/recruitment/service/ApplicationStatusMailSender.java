package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import java.util.UUID;

public interface ApplicationStatusMailSender {
    void send(UUID notificationId, String email, ApplicationStatusHistory history, Application application);
}
