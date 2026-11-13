package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=${HS02_PHASE2_TEST_DB_URL:jdbc:h2:mem:hs02_files;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${HS02_PHASE2_TEST_DB_USER:sa}", "spring.datasource.password=${HS02_PHASE2_TEST_DB_PASSWORD:}",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class ProfileFilesApiTest {
    static final Path storage;
    static { try { storage = Files.createTempDirectory("hs02-upload-test-"); }
        catch (Exception ex) { throw new ExceptionInInitializerError(ex); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("app.profile.files-dir", storage::toString);
    }
    @AfterAll static void cleanup() throws Exception {
        try (var paths = Files.list(storage)) { for (Path p : paths.toList()) Files.deleteIfExists(p); }
        Files.deleteIfExists(storage);
    }
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    UUID owner;
    String token;
    @BeforeEach void setup() { owner = UUID.randomUUID(); token = jwt.generateJwtToken(owner, Role.CANDIDATE, null); }
    @Test void uploadDetectsContentIgnoresClientMimeAndDownloadsOnlyForOwner() throws Exception {
        byte[] content = "%PDF-1.7\nexample certificate".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String url = upload(content, "fake.exe", "image/png");
        assertThat(url).startsWith("/api/v1/files/");
        String path = url.substring("/api/v1".length());
        mvc.perform(get(path).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(content().bytes(content))
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Cache-Control", "private, no-store"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")));
        mvc.perform(get(path).header("Authorization", "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/files/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
    @Test void exactSizeLimitIsAcceptedButOversizeAndForgedTypesLeaveNoRows() throws Exception {
        byte[] maximum = new byte[5 * 1024 * 1024];
        System.arraycopy("%PDF-1.7".getBytes(), 0, maximum, 0, 8);
        upload(maximum, "certificate.pdf", "application/pdf");
        for (byte[] bytes : new byte[][]{new byte[0], "MZexecutable".getBytes(), "random".getBytes()}) {
            mvc.perform(multipart("/files").file(new MockMultipartFile("file", "forged.pdf", "application/pdf", bytes))
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
        mvc.perform(multipart("/files").file(new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[6 * 1024 * 1024]))
            .header("Authorization", "Bearer " + token)).andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        assertThat(jdbc.queryForObject("select count(*) from profile.uploaded_files where user_id=?", Integer.class, owner)).isEqualTo(1);
    }
    @Test void uploadsJpgAndPngByMagicBytes() throws Exception {
        String jpg = upload(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,1}, "photo", "application/octet-stream");
        String png = upload(new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,1}, "photo", "application/octet-stream");
        mvc.perform(get(jpg.substring(7)).header("Authorization", "Bearer " + token)).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        mvc.perform(get(png.substring(7)).header("Authorization", "Bearer " + token)).andExpect(content().contentType(MediaType.IMAGE_PNG));
    }
    @Test void realMultipartParserRejectsSixMegabytesWithStructuredError() throws Exception {
        byte[] prefix = ("--limit\r\nContent-Disposition: form-data; name=\"file\"; filename=\"large.pdf\"\r\n"
            + "Content-Type: application/pdf\r\n\r\n%PDF-1.7").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] ending = "\r\n--limit--\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] body = new byte[prefix.length + 6 * 1024 * 1024 + ending.length];
        System.arraycopy(prefix, 0, body, 0, prefix.length);
        System.arraycopy(ending, 0, body, body.length - ending.length, ending.length);
        var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/files"))
            .header("Authorization", "Bearer " + token).header("Content-Type", "multipart/form-data; boundary=limit")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(body)).build();
        var response = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("FILE_TOO_LARGE");
    }
    @Test void otherRolesCannotUploadOrDownloadAndMissingPartIsBadRequest() throws Exception {
        String url = upload("%PDF-1.7".getBytes(), "a.pdf", "application/pdf");
        for (Role role : Role.values()) {
            if (role == Role.CANDIDATE) continue;
            String other = jwt.generateJwtToken(UUID.randomUUID(), role, null);
            mvc.perform(multipart("/files").file(new MockMultipartFile("file", new byte[]{1})).header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
            mvc.perform(get(url.substring(7)).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        }
        mvc.perform(multipart("/files").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/files").file(new MockMultipartFile("file", new byte[]{1}))).andExpect(status().isUnauthorized());
    }
    @Test void avatarAcceptsOnlyOwnedImagesAndCanBeRemoved() throws Exception {
        jdbc.update("insert into profile.candidate_profiles(user_id,full_name,email,program,class_code,course_name,visibility,updated_at) "
            + "values (?,?,?,?,?,?,?,CURRENT_TIMESTAMP)", owner, "An", "an@example.com", "KS", "K1", "K25", "PRIVATE");
        String image = upload(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,1}, "a.jpg", "image/jpeg");
        mvc.perform(put("/profiles/me/avatar").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("fileUrl", image))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(image));
        String pdf = upload("%PDF-1.7".getBytes(), "a.pdf", "application/pdf");
        mvc.perform(put("/profiles/me/avatar").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("fileUrl", pdf)))).andExpect(status().isBadRequest());
        String otherToken = jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
        mvc.perform(put("/profiles/me/avatar").header("Authorization", "Bearer " + otherToken).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(java.util.Map.of("fileUrl", image)))).andExpect(status().isNotFound());
        mvc.perform(put("/profiles/me/avatar").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fileUrl\":\"https://evil.example/photo.png\"}")).andExpect(status().isNotFound());
        mvc.perform(put("/profiles/me/avatar").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fileUrl\":null}")).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").isEmpty());
    }
    private String upload(byte[] data, String filename, String type) throws Exception {
        String body = mvc.perform(multipart("/files").file(new MockMultipartFile("file", filename, type, data))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("url").asText();
    }
}
