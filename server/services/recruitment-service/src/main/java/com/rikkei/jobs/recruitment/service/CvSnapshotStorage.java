package com.rikkei.jobs.recruitment.service;

import java.util.UUID;

public interface CvSnapshotStorage {
    void store(UUID applicationId, byte[] pdf);
    byte[] read(UUID applicationId);
    void delete(UUID applicationId);
}
