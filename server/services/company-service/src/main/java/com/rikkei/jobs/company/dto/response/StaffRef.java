package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** Cán bộ thực hiện thao tác; {@code fullName} null khi không tra được identity-service. */
public record StaffRef(UUID id, String fullName) {
}
