package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class CompanyLogoApiTest extends CompanyApiTestSupport {

    @Autowired private AppProperties props;

    private Company company;

    @BeforeEach
    void setUp() {
        company = saveCompany("1111111111", CompanyStatus.ACTIVE);
        linkHr(HR1, company.getId());
    }

    private static byte[] png(int size) {
        byte[] b = new byte[size];
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(sig, 0, b, 0, sig.length);
        Arrays.fill(b, sig.length, size, (byte) 7);
        return b;
    }

    private static byte[] jpg(int size) {
        byte[] b = new byte[size];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        Arrays.fill(b, 3, size, (byte) 7);
        return b;
    }

    private ResultActions upload(String token, String name, String type, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/companies/me/logo")
                .file(new MockMultipartFile("file", name, type, bytes))
                .header("Authorization", "Bearer " + token));
    }

    private String hrToken() {
        return token(HR1, Role.HR);
    }

    private String logoUrlOf(ResultActions r) throws Exception {
        String body = r.andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.logoUrl");
    }

    @Test
    void uploadsPngAndServesIt() throws Exception {
        byte[] data = png(1000);
        ResultActions r = upload(hrToken(), "logo.png", "image/png", data).andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.startsWith("/companies/logos/")));
        String url = logoUrlOf(r);
        MvcResult res = mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andReturn();
        assertThat(res.getResponse().getContentAsByteArray()).isEqualTo(data);
        assertThat(res.getResponse().getHeader("Cache-Control")).contains("public").contains("max-age=86400");
    }

    @Test
    void extensionComesFromMagicBytesNotFileName() throws Exception {
        String url = logoUrlOf(upload(hrToken(), "evil.php", "text/html", jpg(100)).andExpect(status().isOk()));
        assertThat(url).endsWith(".jpg");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void exactly2MbAccepted() throws Exception {
        upload(hrToken(), "logo.jpg", "image/jpeg", jpg(2 * 1024 * 1024)).andExpect(status().isOk());
    }

    @Test
    void over2MbRejected() throws Exception {
        upload(hrToken(), "logo.png", "image/png", png(2 * 1024 * 1024 + 1)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void fakePngRejected() throws Exception {
        upload(hrToken(), "logo.png", "image/png", "hello".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void emptyFileRejected() throws Exception {
        upload(hrToken(), "logo.png", "image/png", new byte[0])
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void missingFilePartIs400() throws Exception {
        mvc.perform(multipart("/companies/me/logo").header("Authorization", "Bearer " + hrToken()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void replacingLogoDeletesOldFile() throws Exception {
        String first = logoUrlOf(upload(hrToken(), "a.png", "image/png", png(500)).andExpect(status().isOk()));
        Path firstFile = Path.of(props.getUpload().getDir()).resolve(first.substring("/companies/logos/".length()));
        assertThat(firstFile).exists();
        String second = logoUrlOf(upload(hrToken(), "b.png", "image/png", png(600)).andExpect(status().isOk()));
        assertThat(second).isNotEqualTo(first);
        assertThat(firstFile).doesNotExist();
        mvc.perform(get(first)).andExpect(status().isNotFound());
        mvc.perform(get(second)).andExpect(status().isOk());
        assertThat(Files.exists(Path.of(props.getUpload().getDir())
                .resolve(second.substring("/companies/logos/".length())))).isTrue();
    }

    @Test
    void traversalNameIsNeverServed() throws Exception {
        // StrictHttpFirewall có thể từ chối 400 trước MVC; chấp nhận 400 hoặc 404, không bao giờ 200/500.
        int s = mvc.perform(get("/companies/logos/..%2Fapplication.yml")).andReturn().getResponse().getStatus();
        assertThat(s).isIn(400, 404);
    }

    @Test
    void badOrUnknownNameIs404() throws Exception {
        mvc.perform(get("/companies/logos/abc.gif")).andExpect(status().isNotFound());
        mvc.perform(get("/companies/logos/" + UUID.randomUUID() + ".png")).andExpect(status().isNotFound());
    }

    @Test
    void lockedCompanyCannotUpload() throws Exception {
        company.setStatus(CompanyStatus.LOCKED);
        companies.save(company);
        upload(hrToken(), "logo.png", "image/png", png(100)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
    }

    @Test
    void staffCannotUploadForMe() throws Exception {
        upload(token(Role.STAFF), "logo.png", "image/png", png(100)).andExpect(status().isForbidden());
    }
}
