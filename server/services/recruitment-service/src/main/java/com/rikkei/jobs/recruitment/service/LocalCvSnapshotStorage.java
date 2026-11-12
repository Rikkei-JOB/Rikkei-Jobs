package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalCvSnapshotStorage implements CvSnapshotStorage {
    private final Path root;

    public LocalCvSnapshotStorage(@Value("${app.recruitment.cv-storage-dir:.runtime/recruitment-cvs}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override public void store(UUID applicationId, byte[] pdf) {
        Path temporary = null;
        try {
            Files.createDirectories(root);
            temporary = Files.createTempFile(root, applicationId + "-", ".tmp");
            Files.write(temporary, pdf);
            // Same-volume move, deliberately without REPLACE_EXISTING: snapshots are immutable.
            Files.move(temporary, file(applicationId));
        } catch (IOException ex) {
            throw unavailable();
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { /* Unreferenced temp file only. */ }
            }
        }
    }

    @Override public byte[] read(UUID applicationId) {
        try { return Files.readAllBytes(file(applicationId)); }
        catch (IOException ex) { throw unavailable(); }
    }

    @Override public void delete(UUID applicationId) {
        try { Files.deleteIfExists(file(applicationId)); }
        catch (IOException ex) { throw unavailable(); }
    }

    private Path file(UUID id) { return root.resolve(id + ".pdf"); }
    private RecruitmentDependencyException unavailable() {
        return new RecruitmentDependencyException(503, "SNAPSHOT_UNAVAILABLE", "Không thể lưu hoặc đọc bản CV đã nộp. Vui lòng thử lại.");
    }
}
