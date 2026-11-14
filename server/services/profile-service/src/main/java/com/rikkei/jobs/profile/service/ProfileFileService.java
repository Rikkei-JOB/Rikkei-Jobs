package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.entity.UploadedFile;
import com.rikkei.jobs.profile.repository.UploadedFileRepository;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ProfileFileService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final String URL_PREFIX = "/api/v1/files/";
    private final UploadedFileRepository files;
    private final Path root;
    private final Clock clock;
    public ProfileFileService(UploadedFileRepository files,
        @Value("${app.profile.files-dir}") String directory, @Qualifier("skillProfileClock") Clock clock) {
        this.files = files; this.root = Path.of(directory).toAbsolutePath().normalize(); this.clock = clock;
    }
    @Transactional
    public String upload(UUID owner, MultipartFile file) {
        if (file.getSize() > MAX_BYTES)
            throw new ProfileFileException(413, "FILE_TOO_LARGE", "Tệp vượt quá 5MB.");
        if (file.isEmpty()) throw new BadRequestException("Vui lòng chọn tệp PDF, JPG hoặc PNG.");
        Path destination = null;
        boolean written = false;
        try {
            byte[] bytes = file.getBytes();
            String contentType = detectType(bytes);
            var uploaded = new UploadedFile();
            uploaded.setId(UUID.randomUUID()); uploaded.setUserId(owner);
            uploaded.setContentType(contentType); uploaded.setSizeBytes(bytes.length); uploaded.setCreatedAt(clock.instant());
            Files.createDirectories(root);
            destination = path(uploaded);
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            written = true;
            files.saveAndFlush(uploaded);
            return URL_PREFIX + uploaded.getId();
        } catch (IOException ex) {
            if (written) cleanup(destination);
            throw new ProfileFileException(503, "FILE_STORAGE_UNAVAILABLE", "Không lưu được tệp. Vui lòng thử lại.");
        } catch (RuntimeException ex) {
            if (written) cleanup(destination);
            throw ex;
        }
    }
    public UploadedFile requireOwned(UUID owner, String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) throw notFound();
        try {
            String value = url.substring(URL_PREFIX.length());
            UUID id = UUID.fromString(value);
            if (!id.toString().equals(value)) throw notFound();
            return files.findByIdAndUserId(id, owner).orElseThrow(ProfileFileService::notFound);
        } catch (IllegalArgumentException ex) { throw notFound(); }
    }
    public Download download(UUID owner, UUID id) {
        var file = files.findByIdAndUserId(id, owner).orElseThrow(ProfileFileService::notFound);
        try {
            return new Download(Files.readAllBytes(path(file)), file.getContentType(), id + extension(file.getContentType()));
        } catch (NoSuchFileException ex) { throw notFound(); }
        catch (IOException ex) {
            throw new ProfileFileException(503, "FILE_STORAGE_UNAVAILABLE", "Không đọc được tệp. Vui lòng thử lại.");
        }
    }
    private Path path(UploadedFile file) {
        return root.resolve(file.getId() + extension(file.getContentType()));
    }
    private static String extension(String type) {
        return switch (type) { case "application/pdf" -> ".pdf"; case "image/jpeg" -> ".jpg"; case "image/png" -> ".png";
            default -> throw new BadRequestException("Loại tệp không hợp lệ."); };
    }
    private static String detectType(byte[] bytes) {
        if (startsWith(bytes, new int[]{0x25, 0x50, 0x44, 0x46, 0x2d})) return "application/pdf";
        if (startsWith(bytes, new int[]{0xff, 0xd8, 0xff})) return "image/jpeg";
        if (startsWith(bytes, new int[]{0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) return "image/png";
        throw new BadRequestException("Chỉ nhận tệp PDF, JPG hoặc PNG có nội dung hợp lệ.");
    }
    private static boolean startsWith(byte[] bytes, int[] signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if ((bytes[i] & 0xff) != signature[i]) return false;
        return true;
    }
    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Không tìm thấy tệp."); }
    private static void cleanup(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException ignored) { /* Keep unreferenced files for manual storage reconciliation. */ }
    }
    public record Download(byte[] bytes, String contentType, String filename) { }
}
