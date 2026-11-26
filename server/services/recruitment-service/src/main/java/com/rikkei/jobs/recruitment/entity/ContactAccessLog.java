package com.rikkei.jobs.recruitment.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name="contact_access_logs", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class ContactAccessLog {
    @Id private UUID id;
    private UUID companyId;
    private UUID hrId;
    private UUID userId;
    @Enumerated(EnumType.STRING) private ContactAccessVia via;
    private Instant createdAt;
}
