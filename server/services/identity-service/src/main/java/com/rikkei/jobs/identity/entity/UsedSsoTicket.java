package com.rikkei.jobs.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "used_sso_tickets")
public class UsedSsoTicket implements Persistable<String> {
    @Id
    @Column(name = "ticket", length = 255)
    private String ticket;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @Override
    public String getId() {
        return ticket;
    }

    /** Luôn là INSERT: vé dùng lại sẽ vi phạm khoá chính thay vì bị merge. */
    @Override
    @Transient
    public boolean isNew() {
        return true;
    }
}
