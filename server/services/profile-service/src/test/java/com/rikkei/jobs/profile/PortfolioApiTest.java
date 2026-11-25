package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:hs03_portfolio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class PortfolioApiTest {
    static final Path storage;
    static { try { storage = Files.createTempDirectory("hs03-portfolio-test-"); }
        catch (Exception ex) { throw new ExceptionInInitializerError(ex); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { r.add("app.profile.files-dir", storage::toString); }
    @AfterAll static void cleanup() throws Exception {
        try (var paths = Files.list(storage)) { for (Path p : paths.toList()) Files.deleteIfExists(p); }
        Files.deleteIfExists(storage);
    }
    static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    UUID owner;
    String token;
    @BeforeEach void setup() { owner = UUID.randomUUID(); token = jwt.generateJwtToken(owner, Role.CANDIDATE, null); }

    @Test void personalProjectWithTwoImagesIsListedAndImagesAreReadable() throws Exception {
        String id = create(Map.of("name", "Shop app", "role", "Fullstack", "techStack", List.of("React", " Java "),
            "repoUrl", "https://github.com/me/shop", "demoUrl", "http://shop.example.com/demo"));
        upload(id, PNG).andExpect(status().isCreated());
        upload(id, PNG).andExpect(status().isCreated());
        JsonNode list = mine();
        assertThat(list).hasSize(1);
        JsonNode p = list.get(0);
        assertThat(p.get("source").asText()).isEqualTo("PERSONAL");
        assertThat(p.get("verified").asBoolean()).isFalse();
        assertThat(p.get("techStack").toString()).isEqualTo("[\"React\",\"Java\"]");
        assertThat(p.get("media")).hasSize(2);
        String url = p.get("media").get(0).get("url").asText();
        assertThat(url).startsWith("/api/v1/profiles/" + owner + "/portfolio/media/");
        mvc.perform(get(url.substring(7)).header("Authorization", bearer()))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(content().bytes(PNG));
        // Hồ sơ chưa PUBLIC: người khác không xem được danh sách lẫn ảnh.
        String stranger = "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.HR, null);
        mvc.perform(get("/profiles/" + owner + "/portfolio").header("Authorization", stranger)).andExpect(status().isNotFound());
        mvc.perform(get(url.substring(7)).header("Authorization", stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/profiles/" + owner + "/portfolio").header("Authorization", bearer())).andExpect(status().isOk());
    }

    @Test void iocProjectCannotBeDeletedAndOnlyPresentationFieldsChange() throws Exception {
        UUID ioc = UUID.randomUUID();
        jdbc.update("insert into profile.portfolio_projects(id,user_id,source,internship_id,name,tech_stack,visible,sort_order) "
            + "values (?,?,'IOC',?,'IOC project','Java',true,0)", ioc, owner, UUID.randomUUID());
        mvc.perform(delete("/profiles/me/portfolio/" + ioc).header("Authorization", bearer()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IOC_PROJECT_LOCKED"));
        mvc.perform(put("/profiles/me/portfolio/" + ioc).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("name", "Hacked", "role", "Tech lead", "description", "Mô tả", "visible", false,
                "techStack", List.of("PHP")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("IOC project"))
            .andExpect(jsonPath("$.techStack[0]").value("Java")).andExpect(jsonPath("$.role").value("Tech lead"))
            .andExpect(jsonPath("$.visible").value(false)).andExpect(jsonPath("$.verified").value(true));
        assertThat(mine()).hasSize(1);
    }

    @Test void invalidUrlsAndMissingNameAreRejected() throws Exception {
        for (String bad : List.of("abc", "javascript:alert(1)", "ftp://x.com", "http://")) {
            post(Map.of("name", "P", "repoUrl", bad)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.repoUrl").exists());
            post(Map.of("name", "P", "demoUrl", bad)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.demoUrl").exists());
        }
        post(Map.of("name", " ")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.name").exists());
        post(Map.of("name", "P", "startDate", "2026-05-02", "endDate", "2026-05-01")).andExpect(status().isBadRequest());
        assertThat(mine()).isEmpty();
    }

    @Test void seventhImageOversizeAndNonImageAreRejected() throws Exception {
        String id = create(Map.of("name", "Gallery"));
        for (int i = 0; i < 6; i++) upload(id, PNG).andExpect(status().isCreated());
        upload(id, PNG).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TOO_MANY_IMAGES"));
        String other = create(Map.of("name", "Other"));
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        upload(other, big).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
        upload(other, "%PDF-1.7 doc".getBytes()).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from profile.portfolio_media m join profile.portfolio_projects p on p.id=m.project_id where p.user_id=?",
            Integer.class, owner)).isEqualTo(6);
        // Người khác không tải ảnh vào dự án của mình được.
        mvc.perform(multipart("/profiles/me/portfolio/" + id + "/media").file(new MockMultipartFile("file", "a.png", "image/png", PNG))
            .header("Authorization", "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null))).andExpect(status().isNotFound());
    }

    @Test void reorderPersistsAndRejectsIncompleteLists() throws Exception {
        String a = create(Map.of("name", "A")), b = create(Map.of("name", "B")), c = create(Map.of("name", "C"));
        order(List.of(c, a, b)).andExpect(status().isOk());
        assertThat(mine().findValuesAsText("name")).containsExactly("C", "A", "B");
        order(List.of(c, a)).andExpect(status().isBadRequest());
        order(List.of(c, a, a)).andExpect(status().isBadRequest());
        order(List.of(c, a, UUID.randomUUID().toString())).andExpect(status().isBadRequest());
        assertThat(mine().findValuesAsText("name")).containsExactly("C", "A", "B");
        mvc.perform(delete("/profiles/me/portfolio/" + a).header("Authorization", bearer())).andExpect(status().isNoContent());
        assertThat(mine().findValuesAsText("name")).containsExactly("C", "B");
    }

    String bearer() { return "Bearer " + token; }
    JsonNode mine() throws Exception {
        return json.readTree(mvc.perform(get("/profiles/me/portfolio").header("Authorization", bearer()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    ResultActions post(Map<String, ?> body) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/profiles/me/portfolio")
            .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    String create(Map<String, ?> body) throws Exception {
        return json.readTree(post(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
    }
    ResultActions upload(String projectId, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/profiles/me/portfolio/" + projectId + "/media")
            .file(new MockMultipartFile("file", "photo.png", "image/png", bytes)).header("Authorization", bearer()));
    }
    ResultActions order(List<String> ids) throws Exception {
        return mvc.perform(put("/profiles/me/portfolio/order").header("Authorization", bearer())
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("ids", ids))));
    }
}
