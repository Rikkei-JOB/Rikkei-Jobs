package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

@ExtendWith(OutputCaptureExtension.class)
class StaffCompanyListApiTest extends CompanyApiTestSupport {

    private Company a;
    private Company b;

    @BeforeEach
    void seed() throws Exception {
        a = named("Công ty A", "0101234567", CompanyStatus.ACTIVE);
        Thread.sleep(15);
        b = named("Công ty B", "0109876543210", CompanyStatus.ACTIVE);
        Thread.sleep(15);
        named("Beta 100%", "0312345678", CompanyStatus.LOCKED);
        linkHr(HR1, a.getId());
        linkHr(HR2, a.getId());
    }

    private Company named(String name, String taxCode, CompanyStatus status) {
        Company company = new Company();
        company.setName(name);
        company.setTaxCode(taxCode);
        company.setStatus(status);
        company.setCreatedBy(UUID.randomUUID());
        return companies.save(company);
    }

    private ResultActions list(String query) throws Exception {
        return mvc.perform(get("/staff/companies" + query).header("Authorization", "Bearer " + token(Role.STAFF)));
    }

    private ResultActions detail(UUID id) throws Exception {
        return mvc.perform(get("/staff/companies/" + id).header("Authorization", "Bearer " + token(Role.STAFF)));
    }


    private IdentityUserStatus idUser(UUID id, String email, String status) {
        return new IdentityUserStatus(id, email, "Tên " + email, status, null, null, null);
    }

    @Test
    void searchByNamePart() throws Exception {
        list("?q=ty a").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Công ty A"));
    }

    @Test
    void searchIgnoresCaseWithVietnamese() throws Exception {
        list("?q=CÔNG TY a").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Công ty A"));
        list("?q=công ty a").andExpect(jsonPath("$.items[0].name").value("Công ty A"));
    }

    @Test
    void searchByTaxCodePart() throws Exception {
        list("?q=98765").andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Công ty B"));
        list("?q=0101234567").andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Công ty A"));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        list("?q=%").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Beta 100%"));
        list("?q=_").andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void filterByStatus() throws Exception {
        list("?status=LOCKED").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Beta 100%"));
        list("?status=FOO").andExpect(status().isBadRequest());
    }

    @Test
    void pagingAndCounts() throws Exception {
        list("?size=2&page=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Công ty A"))
                .andExpect(jsonPath("$.items[0].hrCount").value(2))
                .andExpect(jsonPath("$.items[0].jdCount").value(0));
        list("").andExpect(jsonPath("$.items[0].name").value("Beta 100%"))
                .andExpect(jsonPath("$.items[1].name").value("Công ty B"))
                .andExpect(jsonPath("$.items[2].name").value("Công ty A"))
                .andExpect(jsonPath("$.items[1].hrCount").value(0))
                .andExpect(jsonPath("$.items[?(@.jdCount != 0)]").isEmpty());
    }

    @Test
    void jdCountCountsNonDraftJobs() throws Exception {
        saveJob(a.getId(), JobStatus.DRAFT);
        saveJob(a.getId(), JobStatus.PENDING);
        saveJob(a.getId(), JobStatus.APPROVED);

        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + a.getId() + "')].jdCount").value(2))
                .andExpect(jsonPath("$.items[?(@.id == '" + b.getId() + "')].jdCount").value(0));
    }

    @Test
    void listIncludesLogoCityAndSize() throws Exception {
        a.setLogoUrl("/companies/logos/a.png");
        a.setCity("Hà Nội");
        a.setSize("51-200");
        companies.save(a);

        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + a.getId() + "')].logoUrl").value("/companies/logos/a.png"))
                .andExpect(jsonPath("$.items[?(@.id == '" + a.getId() + "')].city").value("Hà Nội"))
                .andExpect(jsonPath("$.items[?(@.id == '" + a.getId() + "')].size").value("51-200"))
                .andExpect(jsonPath("$.items[?(@.id == '" + b.getId() + "')].logoUrl").value((Object) null))
                .andExpect(jsonPath("$.items[?(@.id == '" + b.getId() + "')].city").value((Object) null))
                .andExpect(jsonPath("$.items[?(@.id == '" + b.getId() + "')].size").value((Object) null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0", "size=0", "size=101"})
    void invalidPagingIs400(String query) throws Exception {
        list("?" + query).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void detailListsHrFromIdentity() throws Exception {
        when(identity.lookupUsers(anyList())).thenReturn(List.of(
                new IdentityUserStatus(HR1, "zeta@x.vn", "Zeta", "LOCKED", "Nghỉ việc",
                        Instant.parse("2026-11-01T00:00:00Z"), null),
                new IdentityUserStatus(HR2, "alpha@x.vn", "Alpha", "ACTIVE", null, null,
                        Instant.parse("2099-01-01T00:00:00Z"))));

        detail(a.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.name").value("Công ty A"))
                .andExpect(jsonPath("$.hrAccounts.length()").value(2))
                .andExpect(jsonPath("$.hrAccounts[0].email").value("alpha@x.vn"))
                .andExpect(jsonPath("$.hrAccounts[0].userId").value(HR2.toString()))
                .andExpect(jsonPath("$.hrAccounts[0].fullName").value("Alpha"))
                .andExpect(jsonPath("$.hrAccounts[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.hrAccounts[0].tempLockedUntil").isNotEmpty())
                .andExpect(jsonPath("$.hrAccounts[1].email").value("zeta@x.vn"))
                .andExpect(jsonPath("$.hrAccounts[1].status").value("LOCKED"))
                .andExpect(jsonPath("$.hrAccounts[1].lockedReason").value("Nghỉ việc"))
                .andExpect(jsonPath("$.hrAccounts[1].lockedAt").isNotEmpty());
        verify(identity, times(1)).lookupUsers(anyList());
    }

    @Test
    void qLongerThan100Is400() throws Exception {
        list("?q=" + "a".repeat(101)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        list("?q=" + "a".repeat(100)).andExpect(status().isOk());
    }

    @Test
    void detailSplitsHrLookupInto100PerCall() throws Exception {
        Company big = named("Công ty lớn", "0400000001", CompanyStatus.ACTIVE);
        for (int i = 0; i < 101; i++) {
            linkHr(UUID.randomUUID(), big.getId());
        }
        when(identity.lookupUsers(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            assertThat(ids.size()).isLessThanOrEqualTo(100);
            return ids.stream().map(id -> idUser(id, id + "@x.vn", "ACTIVE")).toList();
        });

        detail(big.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.hrAccounts.length()").value(101));
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(identity, times(2)).lookupUsers(captor.capture());
        assertThat(captor.getAllValues()).extracting(List::size).containsExactlyInAnyOrder(100, 1);
    }

    @Test
    void detailWithoutHrSkipsIdentity() throws Exception {
        detail(b.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.hrAccounts").isArray())
                .andExpect(jsonPath("$.hrAccounts").isEmpty());
        verifyNoInteractions(identity);
    }

    @Test
    void detailDropsUsersMissingInIdentity(CapturedOutput output) throws Exception {
        when(identity.lookupUsers(anyList())).thenReturn(List.of(idUser(HR1, "one@x.vn", "ACTIVE")));
        detail(a.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.hrAccounts.length()").value(1));
        assertThat(output.getOut()).contains("nhưng không còn ở identity").contains(HR2.toString());
    }

    @Test
    void detailIdentityDownIs503() throws Exception {
        when(identity.lookupUsers(anyList())).thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
        detail(a.getId()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));
    }

    @Test
    void detailUnknownIs404() throws Exception {
        detail(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void hrTokenIs403() throws Exception {
        String hr = "Bearer " + token(HR1, Role.HR);
        mvc.perform(get("/staff/companies").header("Authorization", hr)).andExpect(status().isForbidden());
        mvc.perform(get("/staff/companies/" + a.getId()).header("Authorization", hr))
                .andExpect(status().isForbidden());
    }
}
