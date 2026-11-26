package com.rikkei.jobs.recruitment;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
class ContactRequestConcurrencyTest extends ContactIntegrationSupport {
    @Test void onlyOnePendingAndOneReviewWin() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var start=new CountDownLatch(1);
            Callable<Integer> create=()->{start.await();return mvc.perform(post("/candidates/"+user+"/contact-requests")
                .header("Authorization",hr()).contentType("application/json").content(requestBody("Cần phỏng vấn"))).andReturn().getResponse().getStatus();};
            var a=pool.submit(create);var b=pool.submit(create);start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            UUID id=jdbc.queryForObject("select id from recruitment.contact_requests",UUID.class);
            var reviewStart=new CountDownLatch(1);
            Callable<Integer> review=()->{reviewStart.await();return mvc.perform(post("/staff/contact-requests/"+id+"/approve").header("Authorization",staff())).andReturn().getResponse().getStatus();};
            var c=pool.submit(review);var d=pool.submit(review);reviewStart.countDown();
            assertThat(List.of(c.get(15,TimeUnit.SECONDS),d.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        } finally {pool.shutdownNow();}
    }
}
