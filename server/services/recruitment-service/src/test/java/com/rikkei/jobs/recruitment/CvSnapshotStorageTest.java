package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.service.LocalCvSnapshotStorage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class CvSnapshotStorageTest {
    @TempDir Path directory;

    @Test void snapshotIsImmutableAndSurvivesAdapterRestart() {
        var id = UUID.randomUUID();
        var original = "%PDF-1.7\noriginal".getBytes(StandardCharsets.UTF_8);
        var storage = new LocalCvSnapshotStorage(directory.toString());
        storage.store(id, original);
        assertThat(new LocalCvSnapshotStorage(directory.toString()).read(id)).isEqualTo(original);
        assertThatThrownBy(() -> storage.store(id, "%PDF-new".getBytes(StandardCharsets.UTF_8))).isInstanceOf(RuntimeException.class);
        assertThat(storage.read(id)).isEqualTo(original);
        storage.delete(id);
        assertThatThrownBy(() -> storage.read(id)).isInstanceOf(RuntimeException.class);
    }
}
