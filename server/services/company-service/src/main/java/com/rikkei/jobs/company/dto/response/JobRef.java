package com.rikkei.jobs.company.dto.response;

import java.util.UUID;

/** JD mà một thao tác nhắm tới; {@code title} null khi JD không còn. */
public record JobRef(UUID id, String title) {
}
