package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactProfile;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ContactRequestApiTest extends ContactIntegrationSupport {
    @Test void reasonIsTrimmedBeforeLengthValidation() throws Exception {
        mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json")
            .content(requestBody("  "+"x".repeat(2000)+"  "))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.reason").value("x".repeat(2000)));
    }
    String root(){return "/candidates/"+user;}
    UUID create() throws Exception {
        var response=mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json")
            .content(requestBody(" Cần trao đổi vị trí Java "))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.reason").value("Cần trao đổi vị trí Java")).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
    }
    @Test void requestApproveAndStatusContainNoContact() throws Exception {
        UUID id=create();
        mvc.perform(get(root()+"/contact-access").header("Authorization",hr())).andExpect(status().isOk())
            .andExpect(jsonPath("$.canSeeContact").value(false)).andExpect(jsonPath("$.requestStatus").value("PENDING"));
        mvc.perform(post("/staff/contact-requests/"+id+"/approve").header("Authorization",staff())).andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewedBy").value(staff.toString()));
        mvc.perform(get(root()+"/contact-access").header("Authorization",hr())).andExpect(status().isOk())
            .andExpect(jsonPath("$.canSeeContact").value(true)).andExpect(jsonPath("$.via").value("APPROVED_REQUEST"));
        assertThat(count("contact_access_logs")).isZero();
    }
    @Test void duplicatePendingAndRejectedResubmission() throws Exception {
        UUID id=create();
        mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json").content(requestBody("Lý do")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTACT_REQUEST_PENDING"));
        mvc.perform(post("/staff/contact-requests/"+id+"/reject").header("Authorization",staff()).contentType("application/json").content("{\"reason\":\" Chưa phù hợp \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rejectionReason").value("Chưa phù hợp"));
        create();
        mvc.perform(post("/staff/contact-requests/"+id+"/approve").header("Authorization",staff())).andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONTACT_REQUEST_ALREADY_REVIEWED"));
    }
    @Test void validatesOwnershipVisibilityReasonsAndStaffRole() throws Exception {
        for(String reason:List.of("   ","x".repeat(2001))) mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json")
            .content(requestBody(reason))).andExpect(status().isBadRequest());
        mvc.perform(post(root()+"/contact-requests").header("Authorization",token(hr,Role.HR,UUID.randomUUID())).contentType("application/json")
            .content(requestBody("Lý do"))).andExpect(status().isNotFound());
        when(profiles.get(eq(user),anyString())).thenReturn(new ContactProfile(user,"Ứng viên A","PRIVATE"));
        mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json").content(requestBody("Lý do")))
            .andExpect(status().isNotFound());
        for(Role role:List.of(Role.HR,Role.CANDIDATE,Role.ADMIN)) mvc.perform(get("/staff/contact-requests").header("Authorization",token(hr,role,company))).andExpect(status().isForbidden());
    }
    @Test void alreadyAppliedCannotRequestButPrivateDoesNotRevoke() throws Exception {
        apply();
        when(profiles.get(eq(user),anyString())).thenReturn(new ContactProfile(user,"Ứng viên A","PRIVATE"));
        mvc.perform(get(root()+"/contact-access").header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.canSeeContact").value(true));
        mvc.perform(post(root()+"/contact-requests").header("Authorization",hr()).contentType("application/json").content(requestBody("Lý do")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONTACT_ALREADY_ACCESSIBLE"));
    }
}
