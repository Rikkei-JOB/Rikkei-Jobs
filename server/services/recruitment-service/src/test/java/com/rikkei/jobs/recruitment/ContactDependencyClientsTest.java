package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.client.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class ContactDependencyClientsTest {
    @Test void nonPublicReviewStatesRemainValidMetadataForExistingAccess() {
        for(String visibility:java.util.List.of("PENDING","REJECTED")) {
            var builder=RestClient.builder().baseUrl("http://profile");
            var server=MockRestServiceServer.bindTo(builder).build();UUID id=UUID.randomUUID();
            server.expect(anything()).andRespond(withSuccess("{\"userId\":\""+id+"\",\"fullName\":\"A\",\"visibility\":\""+visibility+"\"}",MediaType.APPLICATION_JSON));
            assertThat(new HttpContactProfileClient(builder.build(),"key").get(id,"Bearer token").visibility()).isEqualTo(visibility);
        }
    }
    @Test void profileValidatesIdentityAndInternalKey() {
        var builder=RestClient.builder().baseUrl("http://profile");
        var server=MockRestServiceServer.bindTo(builder).build();
        UUID id=UUID.randomUUID();
        server.expect(requestTo("http://profile/internal/profiles/"+id+"/contact-metadata"))
            .andExpect(header("X-Internal-Key","key"))
            .andRespond(withSuccess("{\"userId\":\""+id+"\",\"fullName\":\"A\",\"visibility\":\"PUBLIC\"}",MediaType.APPLICATION_JSON));
        var client=new HttpContactProfileClient(builder.build(),"key");
        assertThat(client.get(id,"Bearer token").visibility()).isEqualTo("PUBLIC");
        server.verify();server.reset();
        server.expect(anything()).andRespond(withSuccess("{\"userId\":\""+UUID.randomUUID()+"\",\"fullName\":\"A\",\"visibility\":\"PUBLIC\"}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(()->client.get(id,"Bearer token")).isInstanceOf(com.rikkei.jobs.recruitment.config.RecruitmentDependencyException.class);
        server.verify();
    }
}
