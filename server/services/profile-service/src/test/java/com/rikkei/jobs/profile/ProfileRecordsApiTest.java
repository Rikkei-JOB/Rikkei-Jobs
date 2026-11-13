package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${HS02_PHASE2_TEST_DB_URL:jdbc:h2:mem:hs02_records;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${HS02_PHASE2_TEST_DB_USER:sa}", "spring.datasource.password=${HS02_PHASE2_TEST_DB_PASSWORD:}",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class ProfileRecordsApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    String token;
    UUID owner;
    @BeforeEach void owner() {
        owner = UUID.randomUUID();
        token = jwt.generateJwtToken(owner, Role.CANDIDATE, null);
    }
    String education() { return "{\"school\":\"  Rikkei  \",\"major\":\"Software\",\"fromDate\":\"2022-09-01\",\"toDate\":\"2026-06-01\",\"gpa\":3.5,\"note\":\"Graduated\"}"; }
    String certificate() { return "{\"name\":\"  Java  \",\"issuer\":\"Oracle\",\"issuedAt\":\"2024-01-01\",\"expiresAt\":\"2028-01-01\"}"; }
    JsonNode create(String resource, String body) throws Exception {
        return json.readTree(mvc.perform(post("/profiles/me/" + resource).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    @Test void educationCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        String id = create("educations", education()).path("id").asText();
        mvc.perform(get("/profiles/me/educations").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].school").value("Rikkei"));
        mvc.perform(put("/profiles/me/educations/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"school\":\"New school\",\"fromDate\":\"2023-01-01\",\"major\":\"   \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.school").value("New school"))
            .andExpect(jsonPath("$.major").isEmpty()).andExpect(jsonPath("$.gpa").isEmpty());
        mvc.perform(delete("/profiles/me/educations/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/profiles/me/educations").header("Authorization", "Bearer " + token)).andExpect(content().json("[]"));
    }
    @Test void certificateCanBeCreatedReadUpdatedAndDeleted() throws Exception {
        String id = create("certificates", certificate()).path("id").asText();
        mvc.perform(get("/profiles/me/certificates").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].name").value("Java")).andExpect(jsonPath("$[0].fileType").isEmpty());
        mvc.perform(put("/profiles/me/certificates/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"AWS\",\"issuer\":\"Amazon\",\"issuedAt\":\"2025-01-01\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("AWS")).andExpect(jsonPath("$.expiresAt").isEmpty());
        mvc.perform(delete("/profiles/me/certificates/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/profiles/me/certificates").header("Authorization", "Bearer " + token)).andExpect(content().json("[]"));
    }
    @Test void otherCandidatesCannotReadChangeOrDeleteOwnedRecords() throws Exception {
        for (String resource : new String[]{"educations", "certificates"}) {
            String body = resource.equals("educations") ? education() : certificate();
            String id = create(resource, body).path("id").asText();
            String other = jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
            String path = "/profiles/me/" + resource + "/" + id;
            mvc.perform(put(path).header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
            mvc.perform(delete(path).header("Authorization", "Bearer " + other)).andExpect(status().isNotFound());
            mvc.perform(get("/profiles/me/" + resource).header("Authorization", "Bearer " + other)).andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }
    @Test void missingAndForeignIdsReturnSameResponse() throws Exception {
        String id = create("educations", education()).path("id").asText();
        String other = jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
        String foreign = mvc.perform(delete("/profiles/me/educations/" + id).header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(delete("/profiles/me/educations/" + UUID.randomUUID()).header("Authorization", "Bearer " + other))
            .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(foreign)).isEqualTo(json.readTree(missing));
    }
    @Test void validatesSchoolDatesGpaAndCertificateIssuer() throws Exception {
        for (String body : new String[]{education().replace("2026-06-01", "2021-01-01"), education().replace("3.5", "10.1"), education().replace("3.5", "-1"), education().replace("  Rikkei  ", " "), education().replace("2022-09-01", "2022-02-30")}) {
            mvc.perform(post("/profiles/me/educations").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        for (String body : new String[]{certificate().replace("Oracle", " "), certificate().replace("2028-01-01", "2023-01-01"), certificate().replace("2024-01-01", "2024-13-01")}) {
            mvc.perform(post("/profiles/me/certificates").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }
    @Test void onlyCandidateCanUseRecordRoutes() throws Exception {
        for (String resource : new String[]{"educations", "certificates"}) {
            mvc.perform(get("/profiles/me/" + resource)).andExpect(status().isUnauthorized());
            for (Role role : Role.values()) {
                if (role == Role.CANDIDATE) continue;
                String other = jwt.generateJwtToken(UUID.randomUUID(), role, null);
                mvc.perform(get("/profiles/me/" + resource).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
                mvc.perform(post("/profiles/me/" + resource).header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
                    .content(resource.equals("educations") ? education() : certificate())).andExpect(status().isForbidden());
            }
        }
    }
    @Test void requestCannotSelectAnotherOwnerOrCertificateFileType() throws Exception {
        UUID other = UUID.randomUUID();
        String body = education().replace("{", "{\"userId\":\"" + other + "\",");
        create("educations", body);
        mvc.perform(get("/profiles/me/educations").header("Authorization", "Bearer " + token)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/profiles/me/educations").header("Authorization", "Bearer " + jwt.generateJwtToken(other, Role.CANDIDATE, null)))
            .andExpect(content().json("[]"));
        JsonNode c = create("certificates", certificate().replace("{", "{\"fileType\":\"application/x-msdownload\","));
        assertThat(c.path("fileType").isNull()).isTrue();
    }
    @Test void certificateUsesOwnedUploadTypeAndRejectsOtherOwnersFile() throws Exception {
        var file = new MockMultipartFile("file", "renamed.exe", "application/octet-stream", "%PDF-1.7\ncertificate".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String url = json.readTree(mvc.perform(multipart("/files").file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("url").asText();
        String withFile = certificate().replace("{", "{\"fileUrl\":\"" + url + "\",\"fileType\":\"image/png\",");
        JsonNode created = create("certificates", withFile);
        assertThat(created.path("fileType").asText()).isEqualTo("application/pdf");
        String other = jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
        mvc.perform(post("/profiles/me/certificates").header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON).content(withFile))
            .andExpect(status().isNotFound());
        mvc.perform(put("/profiles/me/certificates/" + created.path("id").asText()).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(certificate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fileUrl").isEmpty()).andExpect(jsonPath("$.fileType").isEmpty());
        mvc.perform(get(url.substring("/api/v1".length())).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
    @Test void certificatesRejectUnownedOrExternalUrlsWithoutSaving() throws Exception {
        for (String url : new String[]{"https://example.com/certificate.pdf", "/api/v1/files/" + UUID.randomUUID()}) {
            mvc.perform(post("/profiles/me/certificates").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(certificate().replace("{", "{\"fileUrl\":\"" + url + "\",")))
                .andExpect(status().isNotFound());
        }
        mvc.perform(get("/profiles/me/certificates").header("Authorization", "Bearer " + token)).andExpect(content().json("[]"));
    }
}
