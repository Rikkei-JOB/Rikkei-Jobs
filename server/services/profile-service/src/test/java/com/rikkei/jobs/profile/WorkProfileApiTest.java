package com.rikkei.jobs.profile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Hồ sơ nghề nghiệp: chỉ MENTOR, STAFF, ADMIN; mỗi người chỉ thấy và sửa hồ sơ của mình. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:work_profile;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class WorkProfileApiTest {
    private static final String URL = "/profiles/work/me";

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    UUID mentor;

    @BeforeEach
    void setup() {
        jdbc.update("delete from profile.work_profiles");
        mentor = UUID.randomUUID();
    }

    private String bearer(UUID user, Role role) { return "Bearer " + jwt.generateJwtToken(user, role, null); }

    private ResultActions save(UUID user, Role role, String body) throws Exception {
        return mvc.perform(put(URL).header("Authorization", bearer(user, role)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void emptyProfileBeforeFirstSave() throws Exception {
        mvc.perform(get(URL).header("Authorization", bearer(mentor, Role.MENTOR))).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value(nullValue()))
            .andExpect(jsonPath("$.expertise").isEmpty())
            .andExpect(jsonPath("$.updatedAt").value(nullValue()));
    }

    @Test
    void savesTrimsAndDeduplicatesExpertise() throws Exception {
        save(mentor, Role.MENTOR, """
            {"title":"  Senior Java Engineer ","organization":"Rikkei Soft","yearsExperience":8,"phone":"0912345678",
             "linkedinUrl":"https://linkedin.com/in/mentor","expertise":["Java"," Spring Boot ","java","",
             "Microservices"],"bio":"  Hơn 8 năm kinh nghiệm backend. "}""")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Senior Java Engineer"))
            .andExpect(jsonPath("$.expertise", contains("Java", "Spring Boot", "Microservices")))
            .andExpect(jsonPath("$.bio").value("Hơn 8 năm kinh nghiệm backend."))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        // Ghi đè toàn bộ: trường không gửi trở về trống.
        save(mentor, Role.MENTOR, "{\"title\":\"Tech Lead\",\"expertise\":[]}").andExpect(status().isOk());
        mvc.perform(get(URL).header("Authorization", bearer(mentor, Role.MENTOR)))
            .andExpect(jsonPath("$.title").value("Tech Lead"))
            .andExpect(jsonPath("$.organization").value(nullValue()))
            .andExpect(jsonPath("$.expertise").isEmpty());
    }

    @Test
    void staffAndAdminHaveTheirOwnProfile() throws Exception {
        UUID staff = UUID.randomUUID();
        save(staff, Role.STAFF, "{\"title\":\"Cán bộ tuyển dụng\",\"organization\":\"Phòng Quan hệ doanh nghiệp\"}").andExpect(status().isOk());
        mvc.perform(get(URL).header("Authorization", bearer(staff, Role.STAFF)))
            .andExpect(jsonPath("$.organization").value("Phòng Quan hệ doanh nghiệp"));
        mvc.perform(get(URL).header("Authorization", bearer(UUID.randomUUID(), Role.ADMIN))).andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value(nullValue()));
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        save(mentor, Role.MENTOR, "{\"phone\":\"12345\"}").andExpect(status().isBadRequest());
        save(mentor, Role.MENTOR, "{\"yearsExperience\":61}").andExpect(status().isBadRequest());
        save(mentor, Role.MENTOR, "{\"linkedinUrl\":\"linkedin.com/in/x\"}").andExpect(status().isBadRequest());
        save(mentor, Role.MENTOR, "{\"expertise\":[\"" + "x".repeat(51) + "\"]}").andExpect(status().isBadRequest());
    }

    @Test
    void candidatesAndHrAreForbidden() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL).header("Authorization", bearer(UUID.randomUUID(), Role.CANDIDATE))).andExpect(status().isForbidden());
        mvc.perform(get(URL).header("Authorization", bearer(UUID.randomUUID(), Role.HR))).andExpect(status().isForbidden());
    }
}
