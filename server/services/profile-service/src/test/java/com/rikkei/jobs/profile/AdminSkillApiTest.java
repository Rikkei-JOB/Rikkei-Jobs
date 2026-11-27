package com.rikkei.jobs.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Quản trị danh mục kỹ năng: chỉ ADMIN; mã viết hoa, không trùng, không đổi được. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin_skill;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class AdminSkillApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("delete from profile.skills where code like 'ADMIN_TEST%'");
    }

    private String bearer(Role role) { return "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), role, null); }

    private ResultActions create(Role role, String body) throws Exception {
        return mvc.perform(post("/admin/skills").header("Authorization", bearer(role)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void createsUpdatesAndRejectsDuplicates() throws Exception {
        String json = create(Role.ADMIN, "{\"code\":\"admin_test_rust\",\"name\":\" Rust \",\"group\":\"Backend\"}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("ADMIN_TEST_RUST"))
            .andExpect(jsonPath("$.name").value("Rust"))
            .andReturn().getResponse().getContentAsString();
        String id = json.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
        create(Role.ADMIN, "{\"code\":\"ADMIN_TEST_RUST\",\"name\":\"Rust 2\",\"group\":\"Backend\"}").andExpect(status().isConflict());
        create(Role.ADMIN, "{\"code\":\"có dấu\",\"name\":\"X\",\"group\":\"Y\"}").andExpect(status().isBadRequest());
        mvc.perform(put("/admin/skills/" + id).header("Authorization", bearer(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Rust (systems)\",\"group\":\"Hệ thống\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("ADMIN_TEST_RUST"))
            .andExpect(jsonPath("$.group").value("Hệ thống"));
        mvc.perform(put("/admin/skills/" + UUID.randomUUID()).header("Authorization", bearer(Role.ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"group\":\"B\"}")).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdmins() throws Exception {
        create(Role.STAFF, "{\"code\":\"ADMIN_TEST_X\",\"name\":\"X\",\"group\":\"Y\"}").andExpect(status().isForbidden());
        create(Role.CANDIDATE, "{\"code\":\"ADMIN_TEST_X\",\"name\":\"X\",\"group\":\"Y\"}").andExpect(status().isForbidden());
    }
}
