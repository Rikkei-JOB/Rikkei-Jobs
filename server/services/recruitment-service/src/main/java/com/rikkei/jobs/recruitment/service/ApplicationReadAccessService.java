package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.recruitment.entity.Application;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ApplicationReadAccessService {
    private final ApplicationRepository applications;
    @Transactional(readOnly=true)
    public Application findReadable(AuthPrincipal actor,UUID applicationId) {
        if(actor==null || (actor.getRole()!=Role.HR && actor.getRole()!=Role.STAFF && actor.getRole()!=Role.ADMIN))
            throw new ForbiddenException("Bạn không có quyền xem đơn này.");
        var application=applications.findById(applicationId)
            .orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
        if(actor.getRole()==Role.HR && !application.getCompanyId().equals(actor.getCompanyId()))
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển.");
        return application;
    }
}
