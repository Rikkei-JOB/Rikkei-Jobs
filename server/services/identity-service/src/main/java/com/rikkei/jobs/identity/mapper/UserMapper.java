package com.rikkei.jobs.identity.mapper;

import com.rikkei.jobs.identity.dto.response.UserResponse;
import com.rikkei.jobs.identity.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .companyId(user.getCompanyId())
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }
}
