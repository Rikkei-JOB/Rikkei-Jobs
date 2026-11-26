package com.rikkei.jobs.company;
import com.rikkei.jobs.company.controller.InternalContactDirectoryController;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.client.*;
import com.rikkei.jobs.company.entity.Company;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class InternalContactDirectoryApiTest {
    @Test void directoryReturnsNamesAndAllowsMissingIds() {
        var companies=mock(CompanyRepository.class);var identity=mock(IdentityClient.class);
        UUID company=UUID.randomUUID(),hr=UUID.randomUUID();var entity=new Company();entity.setId(company);entity.setName("Công ty A");
        when(companies.findAllById(Set.of(company))).thenReturn(List.of(entity));
        when(identity.lookupUsers(List.of(hr))).thenReturn(List.of(new IdentityUserStatus(hr,"private@test","HR A","ACTIVE",null,null,null)));
        var result=new InternalContactDirectoryController(companies,identity).lookup(new InternalContactDirectoryController.Lookup(Set.of(company),Set.of(hr)));
        assertThat(result.companies()).containsEntry(company,"Công ty A");
        assertThat(result.hrs()).containsEntry(hr,"HR A");
    }
}
