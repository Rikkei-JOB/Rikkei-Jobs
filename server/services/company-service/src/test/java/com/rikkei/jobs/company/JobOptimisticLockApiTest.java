package com.rikkei.jobs.company;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.service.JobService;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Hai lần lưu JD chồng nhau (@Version của Job): lỗi khoá lạc quan phải ra 409 CONFLICT, không phải 500. */
class JobOptimisticLockApiTest extends CompanyApiTestSupport {

    @MockBean JobService jobService;

    @Test
    void optimisticLockFailureIs409Conflict() throws Exception {
        UUID id = UUID.randomUUID();
        when(jobService.submit(any(), any())).thenThrow(new ObjectOptimisticLockingFailureException(Job.class, id));

        mvc.perform(post("/jobs/" + id + "/submit").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("JD vừa được cập nhật ở nơi khác, vui lòng tải lại"));
    }
}
