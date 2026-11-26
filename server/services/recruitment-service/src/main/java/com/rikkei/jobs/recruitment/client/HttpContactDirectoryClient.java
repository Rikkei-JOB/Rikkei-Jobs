package com.rikkei.jobs.recruitment.client;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactDirectory;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.client.*;
@Component
public class HttpContactDirectoryClient implements ContactDirectoryClient {
    private final RestClient client; private final String key;
    @Autowired public HttpContactDirectoryClient(RestClient.Builder builder,
        @Value("${app.recruitment.company-base-url}") String url,
        @Value("${app.recruitment.company-service-key:}") String key) {
        this(RecruitmentHttpSupport.client(builder,url),key);
    }
    public HttpContactDirectoryClient(RestClient client,String key){this.client=client;this.key=key;}
    public ContactDirectory lookup(Set<UUID> companies,Set<UUID> hrs) {
        if(companies.isEmpty() && hrs.isEmpty())return new ContactDirectory(Map.of(),Map.of());
        if(key==null || key.isBlank())throw failure();
        try {
            var result=client.post().uri("/internal/contact-directory/lookup").header("X-Internal-Key",key)
                .body(Map.of("companyIds",companies,"hrIds",hrs)).retrieve().body(ContactDirectory.class);
            if(result==null || result.companies()==null || result.hrs()==null)throw failure();
            return result;
        }catch(RestClientException e){throw failure();}
    }
    private RecruitmentDependencyException failure(){return new RecruitmentDependencyException(503,"COMPANY_UNAVAILABLE","Không lấy được tên công ty/HR.");}
}
