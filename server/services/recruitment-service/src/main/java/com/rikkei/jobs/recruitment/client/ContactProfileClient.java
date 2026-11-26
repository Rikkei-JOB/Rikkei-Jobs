package com.rikkei.jobs.recruitment.client;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactProfile;
import java.util.UUID;
public interface ContactProfileClient { ContactProfile get(UUID userId,String bearer); }
