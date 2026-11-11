package com.rikkei.jobs.identity.service;

public interface SsoTicketService {
    /** Đánh dấu vé đã dùng. Ném TICKET_ALREADY_USED nếu vé đã được dùng trước đó. */
    void consume(String ticket);
}
