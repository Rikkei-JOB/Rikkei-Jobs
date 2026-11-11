package com.rikkei.jobs.identity.controller;

import com.rikkei.jobs.identity.dto.request.InternalCreateUserRequest;
import com.rikkei.jobs.identity.dto.response.InternalUserResponse;
import com.rikkei.jobs.identity.service.InternalUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** API nội bộ cho company-service; được InternalKeyFilter bảo vệ bằng X-Internal-Key và gateway không định tuyến tới. */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final InternalUserService internalUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InternalUserResponse create(@Valid @RequestBody InternalCreateUserRequest request) {
        return internalUserService.createUser(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        internalUserService.deleteHrUser(id);
    }
}
