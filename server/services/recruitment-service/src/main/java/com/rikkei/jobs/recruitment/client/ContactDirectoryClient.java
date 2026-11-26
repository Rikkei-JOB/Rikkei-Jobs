package com.rikkei.jobs.recruitment.client;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactDirectory;
import java.util.*;
public interface ContactDirectoryClient { ContactDirectory lookup(Set<UUID> companyIds,Set<UUID> hrIds); }
