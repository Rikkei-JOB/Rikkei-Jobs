package com.rikkei.jobs.profile;
import com.rikkei.jobs.profile.controller.InternalCandidateContactController;
import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class InternalContactProfileApiTest {
    @Test void metadataRequiresKeyAndContainsNoContact() {
        var profiles=mock(CandidateProfileRepository.class);
        var controller=new InternalCandidateContactController(profiles,"test-key");
        UUID id=UUID.randomUUID();
        var profile=new CandidateProfile(); profile.setUserId(id);profile.setFullName("Ứng viên");profile.setVisibility("PUBLIC");
        when(profiles.findById(id)).thenReturn(Optional.of(profile));
        assertThatThrownBy(()->controller.metadata(id,"wrong")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var result=controller.metadata(id,"test-key");
        assertThat(result.userId()).isEqualTo(id);
        assertThat(result.visibility()).isEqualTo("PUBLIC");
        assertThatThrownBy(()->controller.metadata(UUID.randomUUID(),"test-key")).isInstanceOf(com.rikkei.jobs.common.config.exception.ResourceNotFoundException.class);
    }
}
