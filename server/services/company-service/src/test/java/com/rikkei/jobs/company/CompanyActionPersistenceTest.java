package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyActionRepository;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class CompanyActionPersistenceTest extends CompanyApiTestSupport {

    @Autowired CompanyActionRepository actions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void savesActionWithDefaults() {
        Company company = saveCompany("0300000001", CompanyStatus.ACTIVE);
        CompanyAction saved = actions.save(new CompanyAction(
                company.getId(), CompanyActionType.LOCK, null, "Vi phạm", UUID.randomUUID()));

        CompanyAction found = actions.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getAction()).isEqualTo(CompanyActionType.LOCK);
        assertThat(found.getReason()).isEqualTo("Vi phạm");
    }

    @Test
    void rejectsUnknownAction() {
        Company company = saveCompany("0300000002", CompanyStatus.ACTIVE);
        assertThatThrownBy(() -> jdbc.update(
                "insert into company.company_actions (id, company_id, action, staff_id) values (?, ?, 'FOO', ?)",
                UUID.randomUUID(), company.getId(), UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void latestLockFound() {
        Company company = saveCompany("0300000003", CompanyStatus.ACTIVE);
        UUID staff = UUID.randomUUID();
        CompanyAction older = actions.save(new CompanyAction(company.getId(), CompanyActionType.LOCK, null, "Cũ", staff));
        CompanyAction newer = actions.save(new CompanyAction(company.getId(), CompanyActionType.LOCK, null, "Mới", staff));
        jdbc.update("update company.company_actions set created_at = ? where id = ?",
                Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")), older.getId());
        jdbc.update("update company.company_actions set created_at = ? where id = ?",
                Timestamp.from(Instant.parse("2026-02-01T00:00:00Z")), newer.getId());

        assertThat(actions.findFirstByCompanyIdAndActionOrderByCreatedAtDescIdDesc(
                company.getId(), CompanyActionType.LOCK)).get().extracting(CompanyAction::getReason).isEqualTo("Mới");
    }
}
