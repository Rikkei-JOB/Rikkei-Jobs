package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.service.LogoStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class FileSystemLogoStorage implements LogoStorage {

    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png)$");
    private static final Pattern EXTENSION = Pattern.compile("^(jpg|png)$");

    private final Path root;

    public FileSystemLogoStorage(AppProperties props) {
        this.root = Path.of(props.getUpload().getDir()).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String extension) {
        if (!EXTENSION.matcher(extension).matches()) {
            throw new IllegalArgumentException("Phần mở rộng không hợp lệ");
        }
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            Files.write(resolve(fileName), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được tệp logo", e);
        }
        return fileName;
    }

    @Override
    public Optional<Resource> load(String fileName) {
        Path file = safeResolve(fileName);
        if (file == null || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(file));
    }

    @Override
    public void delete(String fileName) {
        Path file = safeResolve(fileName);
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Không xoá được tệp logo", e);
        }
    }

    private Path safeResolve(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            return null;
        }
        return resolve(fileName);
    }

    /** Chuẩn hoá rồi kiểm tra vẫn nằm trong thư mục upload (phòng thủ thêm ngoài regex tên tệp). */
    private Path resolve(String fileName) {
        Path file = root.resolve(fileName).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ");
        }
        return file;
    }
}
