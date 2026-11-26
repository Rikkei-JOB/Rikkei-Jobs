package com.rikkei.jobs.recruitment.client;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactProfile;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.client.*;
@Component
public class HttpContactProfileClient implements ContactProfileClient {
    private final RestClient client;
    private final String key;
    @Autowired public HttpContactProfileClient(RestClient.Builder builder,
        @Value("${app.recruitment.profile-base-url}") String url,
        @Value("${app.recruitment.profile-service-key:}") String key) {
        this(RecruitmentHttpSupport.client(builder,url),key);
    }
    public HttpContactProfileClient(RestClient client,String key) {this.client=client;this.key=key;}
    public ContactProfile get(UUID userId,String bearer) {
        if(key==null || key.isBlank()) throw failure();
        try {
            var p=client.get().uri("/internal/profiles/{id}/contact-metadata",userId)
                .header("X-Internal-Key",key).header("Authorization",bearer).retrieve().body(ContactProfile.class);
            if(p==null || !userId.equals(p.userId()) || p.fullName()==null ||
                !Set.of("PUBLIC","PRIVATE","PENDING","REJECTED").contains(p.visibility()==null?"":p.visibility())) throw failure();
            return p;
        } catch(RestClientResponseException e) {
            if(e.getStatusCode().value()==404)throw new ResourceNotFoundException("Không tìm thấy ứng viên.");
            throw failure();
        } catch(RestClientException e) {throw failure();}
    }
    private RecruitmentDependencyException failure(){return new RecruitmentDependencyException(503,"PROFILE_UNAVAILABLE","Không lấy được hồ sơ ứng viên.");}
}
