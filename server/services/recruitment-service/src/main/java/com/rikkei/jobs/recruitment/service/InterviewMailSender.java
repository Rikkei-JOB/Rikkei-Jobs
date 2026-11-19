package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.entity.InterviewNotificationOutbox;
public interface InterviewMailSender { void send(InterviewNotificationOutbox notification); }
