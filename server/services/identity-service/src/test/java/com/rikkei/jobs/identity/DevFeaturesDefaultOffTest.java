package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.identity.config.DevDataSeeder;
import com.rikkei.jobs.identity.controller.DevSsoController;
import com.rikkei.jobs.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Không bật profile "dev" và không đặt biến môi trường: endpoint cấp vé không cần xác thực và seed tài khoản
 * mật khẩu đã biết phải TẮT, để một bản triển khai quên cấu hình vẫn an toàn.
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-defaults;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.mail.enabled=false"
        })
@AutoConfigureMockMvc
class DevFeaturesDefaultOffTest {

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @Autowired UserRepository users;

    @Test
    void devTicketEndpointIsNotRegisteredByDefault() throws Exception {
        assertThat(context.getBeansOfType(DevSsoController.class)).isEmpty();
        mvc.perform(post("/auth/dev/sso-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lmsUserId\":\"lms-sv-001\"}"))
                // MockMvc trả 404; server thật trả 401 (request chuyển sang /error, trang này cần đăng nhập). Cả hai đều là "không xin được vé".
                .andExpect(status().is4xxClientError());
    }

    @Test
    void sampleAccountsAreNotSeededByDefault() {
        assertThat(context.getBeansOfType(DevDataSeeder.class)).isEmpty();
        assertThat(users.findByEmail("hr1@rikkeijobs.test")).isEmpty();
    }
}
