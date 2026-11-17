package com.rikkei.jobs.recruitment.client;

import java.util.UUID;

/** Current administrative lock state; read from identity for every HR request. */
public interface HrAccountStatusClient {
    enum State { ACTIVE, LOCKED }
    State state(UUID userId, UUID companyId);
}
