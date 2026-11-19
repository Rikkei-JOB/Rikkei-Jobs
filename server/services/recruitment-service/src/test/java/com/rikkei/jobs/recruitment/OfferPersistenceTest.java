package com.rikkei.jobs.recruitment;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

class OfferPersistenceTest extends ApplicationIntegrationSupport {
    @Test void oneOfferPerApplicationAndPositiveSalary() throws Exception {
        UUID applicationId = apply();
        insertOffer(applicationId, 12000000L);
        assertEquals(1, count("offers"));
        assertThrows(DataIntegrityViolationException.class, () -> insertOffer(applicationId, 13000000L));
        assertThrows(DataIntegrityViolationException.class, () -> insertOffer(applicationId, 0L));
    }

    @Test void oneEmploymentRecordPerApplication() throws Exception {
        UUID applicationId = apply();
        insertRecord(applicationId);
        assertEquals(1, count("employment_records"));
        assertThrows(DataIntegrityViolationException.class, () -> insertRecord(applicationId));
    }

    @Test void declinedOfferRequiresReasonAtDatabaseLevel() throws Exception {
        UUID applicationId = apply();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
            insert into recruitment.offers
            (id,application_id,position,salary,start_date,work_location,response_deadline,status,responded_at,created_at)
            values (?,?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), applicationId, "Java Developer", 12000000L,
            Date.valueOf("2030-02-01"), "Hà Nội", Timestamp.from(NOW.plusSeconds(86400)),
            "DECLINED", Timestamp.from(NOW), Timestamp.from(NOW)));
    }

    private void insertOffer(UUID applicationId, long salary) {
        jdbc.update("""
            insert into recruitment.offers
            (id,application_id,position,salary,start_date,work_location,response_deadline,status,created_at)
            values (?,?,?,?,?,?,?,?,?)
            """, UUID.randomUUID(), applicationId, "Java Developer", salary,
            Date.valueOf("2030-02-01"), "Hà Nội", Timestamp.from(NOW.plusSeconds(86400)),
            "SENT", Timestamp.from(NOW));
    }

    private void insertRecord(UUID applicationId) {
        jdbc.update("""
            insert into recruitment.employment_records
            (id,application_id,user_id,company_id,job_id,position,start_date)
            values (?,?,?,?,?,?,?)
            """, UUID.randomUUID(), applicationId, user, company, job,
            "Java Developer", Date.valueOf("2030-02-01"));
    }
}
