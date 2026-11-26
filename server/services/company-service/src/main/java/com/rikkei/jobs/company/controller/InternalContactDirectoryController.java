package com.rikkei.jobs.company.controller;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.client.IdentityClient;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** InternalKeyFilter protects this endpoint; no contact fields are returned. */
@RestController @RequestMapping("/internal/contact-directory") @RequiredArgsConstructor
public class InternalContactDirectoryController {
    private final CompanyRepository companies;
    private final IdentityClient identity;
    public record Lookup(@NotNull @Size(max=100) Set<@NotNull UUID> companyIds,
                         @NotNull @Size(max=100) Set<@NotNull UUID> hrIds) {}
    public record Directory(Map<UUID,String> companies, Map<UUID,String> hrs) {}
    @PostMapping("/lookup")
    public Directory lookup(@Valid @RequestBody Lookup input) {
        var names=companies.findAllById(input.companyIds()).stream()
            .collect(Collectors.toMap(c->c.getId(),c->c.getName()));
        Map<UUID,String> hrs=new HashMap<>();
        if(!input.hrIds().isEmpty()) identity.lookupUsers(new ArrayList<>(input.hrIds())).forEach(u->{
            if(u.fullName()!=null && input.hrIds().contains(u.userId())) hrs.put(u.userId(),u.fullName());
        });
        return new Directory(names,hrs);
    }
}
