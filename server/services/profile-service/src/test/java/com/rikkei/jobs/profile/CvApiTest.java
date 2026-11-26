package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:hs04_cv;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "app.profile.web-base-url=https://jobs.example.test/",
    "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class CvApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    UUID owner, java, english;
    String bearer;

    @BeforeEach void setup() {
        owner = UUID.randomUUID();
        bearer = "Bearer " + jwt.generateJwtToken(owner, Role.CANDIDATE, null);
        java = jdbc.queryForObject("select id from profile.skills where code='JAVA'", UUID.class);
        english = jdbc.queryForObject("select id from profile.skills where code='ENGLISH'", UUID.class);
        jdbc.update("insert into profile.candidate_profiles(user_id,full_name,email,phone,program,class_code,course_name,summary,visibility,updated_at) "
            + "values (?,'Nguyễn Thị Ánh','anh@example.com','0912345678','Java Fullstack','JV01','Khoá 2026','Thích <b>Java</b> & Spring','PRIVATE',CURRENT_TIMESTAMP)", owner);
        jdbc.update("insert into profile.skill_scores(user_id,skill_id,verified_score,level,updated_at) values (?,?,82,'GOOD',CURRENT_TIMESTAMP)", owner, java);
        jdbc.update("insert into profile.skill_scores(user_id,skill_id,self_score,updated_at) values (?,?,70,CURRENT_TIMESTAMP)", owner, english);
    }

    @Test void newCvHasEveryDefaultSectionAndListMatchesUt01Contract() throws Exception {
        JsonNode cv = create("CV Backend", "Java Developer");
        assertThat(cv.get("publicUrl").asText()).matches("https://jobs\\.example\\.test/p/[A-Za-z0-9_-]{32}");
        List<String> types = new ArrayList<>();
        cv.get("sections").forEach(s -> types.add(s.get("section").asText()));
        assertThat(types).containsExactly("SUMMARY", "SKILLS", "LANGUAGES", "INTERNSHIP", "PROJECTS", "EDUCATION", "CERTIFICATES");
        JsonNode skills = section(cv, "SKILLS");
        assertThat(skills.get("items")).hasSize(2);
        assertThat(skills.get("items").get(0).get("verified").isBoolean()).isTrue();
        assertThat(section(cv, "SUMMARY").get("text").asText()).isEqualTo("Thích <b>Java</b> & Spring");

        JsonNode list = json.readTree(mvc.perform(get("/profile/cvs").header("Authorization", bearer))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
            .andReturn().getResponse().getContentAsString(UTF_8));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("id").asText()).isEqualTo(cv.get("id").asText());
        assertThat(list.get(0).get("targetPosition").asText()).isEqualTo("Java Developer");
    }

    @Test void sixthCvIsRejected() throws Exception {
        for (int i = 0; i < 5; i++) create("CV " + i, null);
        mvc.perform(post("/profile/cvs").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CV 6\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CV_LIMIT_REACHED"));
    }

    @Test void pdfIsVietnameseRikkeiCvWithOnlySelectedItemsAndSkillProvenance() throws Exception {
        String id = create("CV Backend", "Java Developer").get("id").asText();
        // Chỉ giữ Java, tắt phần giới thiệu, đưa kỹ năng lên đầu.
        var sections = new ArrayList<Map<String, Object>>();
        sections.add(Map.of("section", "SKILLS", "included", true, "itemIds", List.of(java)));
        for (String t : List.of("SUMMARY", "LANGUAGES", "INTERNSHIP", "PROJECTS", "EDUCATION", "CERTIFICATES")) {
            sections.add(Map.of("section", t, "included", !t.equals("SUMMARY")));
        }
        JsonNode updated = json.readTree(mvc.perform(put("/profile/cvs/" + id + "/sections").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("sections", sections))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(UTF_8));
        assertThat(updated.get("sections").get(0).get("section").asText()).isEqualTo("SKILLS");
        assertThat(updated.get("sections").get(0).get("allItems").asBoolean()).isFalse();

        byte[] pdf = mvc.perform(get("/profile/cvs/" + id + "/pdf").header("Authorization", bearer).accept(MediaType.APPLICATION_PDF))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"cv-" + id + ".pdf\""))
            .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String text;
        try (var doc = PDDocument.load(pdf)) { text = new PDFTextStripper().getText(doc); }
        assertThat(text).contains("Nguyễn Thị Ánh", "anh@example.com", "Java 82", "Verified by Rikkei");
        // Tiêu đề in hoa có giãn chữ, trình trích văn bản có thể chèn khoảng trắng giữa các chữ cái.
        assertThat(text.replace(" ", "")).contains("JAVADEVELOPER", "SKILLS");
        assertThat(text).doesNotContain("Tiếng Anh", "Thích");
    }

    @Test void previewRendersUnsavedDraftWithoutSavingIt() throws Exception {
        String id = create("CV Backend", "Java Developer").get("id").asText();
        var sections = new ArrayList<Map<String, Object>>();
        sections.add(Map.of("section", "SKILLS", "included", true, "itemIds", List.of(java)));
        for (String t : List.of("SUMMARY", "LANGUAGES", "INTERNSHIP", "PROJECTS", "EDUCATION", "CERTIFICATES")) {
            sections.add(Map.of("section", t, "included", true));
        }
        byte[] pdf = mvc.perform(post("/profile/cvs/" + id + "/preview").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("sections", sections))))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();
        String text;
        try (var doc = PDDocument.load(pdf)) { text = new PDFTextStripper().getText(doc); }
        assertThat(text).contains("Java 82");

        JsonNode saved = json.readTree(mvc.perform(get("/profile/cvs/" + id).header("Authorization", bearer))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(UTF_8));
        assertThat(section(saved, "SKILLS").get("allItems").asBoolean()).isTrue();

        mvc.perform(post("/profile/cvs/" + id + "/preview").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("sections", sections.subList(0, 2)))))
            .andExpect(status().isBadRequest());
    }

    @Test void otherCandidateCannotReadEditOrExportTheCv() throws Exception {
        String id = create("CV Backend", null).get("id").asText();
        String other = "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
        mvc.perform(get("/profile/cvs/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(get("/profile/cvs/" + id + "/pdf").header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(delete("/profile/cvs/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        String hr = "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.HR, null);
        mvc.perform(get("/profile/cvs").header("Authorization", hr)).andExpect(status().isForbidden());
        mvc.perform(delete("/profile/cvs/" + id).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/profile/cvs/" + id).header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test void publicPageNeedsNoLoginShowsOnlyVerifiedDataAndRespectsVisibility() throws Exception {
        String url = create("CV Backend", null).get("publicUrl").asText();
        String path = "/profile/public/" + url.substring(url.lastIndexOf('/') + 1);
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.visible").value(false))
            .andExpect(jsonPath("$.fullName").doesNotExist());

        jdbc.update("update profile.candidate_profiles set visibility='PUBLIC',consent_at=CURRENT_TIMESTAMP where user_id=?", owner);
        String body = mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.visible").value(true))
            .andExpect(jsonPath("$.fullName").value("Nguyễn Thị Ánh"))
            .andExpect(jsonPath("$.skills.length()").value(1)).andExpect(jsonPath("$.skills[0].level").value("Tốt"))
            .andReturn().getResponse().getContentAsString(UTF_8);
        assertThat(body).doesNotContain("anh@example.com", "0912345678");
        mvc.perform(get("/profile/public/khong-ton-tai")).andExpect(status().isNotFound());
    }

    private JsonNode create(String name, String position) throws Exception {
        var body = position == null ? Map.of("name", name) : Map.of("name", name, "targetPosition", position);
        return json.readTree(mvc.perform(post("/profile/cvs").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(UTF_8));
    }

    private static JsonNode section(JsonNode cv, String type) {
        for (JsonNode s : cv.get("sections")) if (s.get("section").asText().equals(type)) return s;
        throw new AssertionError("missing section " + type);
    }
}
