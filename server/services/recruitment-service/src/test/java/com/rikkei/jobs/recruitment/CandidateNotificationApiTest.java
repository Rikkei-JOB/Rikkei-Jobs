package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import com.rikkei.jobs.recruitment.service.CandidateNotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateNotificationApiTest extends ApplicationIntegrationSupport {
    static final String FEED = "/recruitment/me/notifications";
    @Autowired ApplicationTransitionService transitions;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CandidateNotificationService notifications;

    @Test void listsOnlyOwnedEventsAndNeverExposesHrNotes() throws Exception {
        UUID id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), "PRIVATE HR NOTE");
        var result = mvc.perform(get(FEED).header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1))
            .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].href").value("/candidate/applications/" + id))
            .andReturn().getResponse().getContentAsString();
        assertFalse(result.contains("PRIVATE HR NOTE"));
        mvc.perform(get(FEED).header("Authorization", token(UUID.randomUUID(), Role.CANDIDATE, null)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test void readOperationsAreOwnedIdempotentAndCountsAreGlobalAcrossPages() throws Exception {
        UUID id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
        transitions.transition(id, ApplicationStatus.INTERVIEW, UUID.randomUUID(), null);
        var text = mvc.perform(get(FEED).param("size", "1").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.unreadCount").value(2))
            .andExpect(jsonPath("$.items.length()").value(1)).andReturn().getResponse().getContentAsString();
        String notification = json.readTree(text).get("items").get(0).get("id").asText();
        mvc.perform(post(FEED + "/{id}/read", notification).header("Authorization", token(UUID.randomUUID(), Role.CANDIDATE, null)))
            .andExpect(status().isNotFound());
        for (int i = 0; i < 2; i++) mvc.perform(post(FEED + "/{id}/read", notification).header("Authorization", candidate())).andExpect(status().isNoContent());
        mvc.perform(get(FEED).param("unreadOnly", "true").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.unreadCount").value(1));
        mvc.perform(post(FEED + "/read-all").header("Authorization", token(UUID.randomUUID(), Role.CANDIDATE, null))).andExpect(status().isNoContent());
        mvc.perform(get(FEED).param("page", "2").param("size", "1").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));
        for (int i = 0; i < 2; i++) mvc.perform(post(FEED + "/read-all").header("Authorization", candidate())).andExpect(status().isNoContent());
        mvc.perform(get(FEED).header("Authorization", candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test void validatesPaginationAndRejectsWrongRoles() throws Exception {
        mvc.perform(get(FEED)).andExpect(status().isUnauthorized());
        for (Role role : new Role[]{Role.HR, Role.STAFF, Role.ADMIN, Role.MENTOR})
            mvc.perform(get(FEED).header("Authorization", token(UUID.randomUUID(), role, company))).andExpect(status().isForbidden());
        for (String query : new String[]{"?page=0", "?size=0", "?size=101", "?page=abc", "?unreadOnly=invalid"})
            mvc.perform(get(FEED + query).header("Authorization", candidate())).andExpect(status().isBadRequest());
        mvc.perform(post(FEED + "/{id}/read", UUID.randomUUID()).header("Authorization", candidate())).andExpect(status().isNotFound());
    }

    @Test void rolledBackStatusChangeDoesNotLeaveAnInboxEvent() throws Exception {
        UUID id = apply();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
            tx.setRollbackOnly();
        });
        mvc.perform(get(FEED).header("Authorization", candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
        mvc.perform(get(FEED).header("Authorization", candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test void publishingTheSameBusinessEventIsIdempotent() throws Exception {
        UUID id = apply();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            for (int i = 0; i < 2; i++) notifications.publish(user, id, "same-event", "APPLICATION_STATUS", "Cập nhật", "Hồ sơ của bạn", "/candidate/applications/" + id, NOW);
        });
        mvc.perform(get(FEED).header("Authorization", candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }
}
