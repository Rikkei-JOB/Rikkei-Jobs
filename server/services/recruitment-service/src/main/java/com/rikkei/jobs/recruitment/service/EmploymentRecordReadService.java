package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.dto.response.EmploymentRecordResponse;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.EmploymentRecordRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class EmploymentRecordReadService {
    private final EmploymentRecordRepository records;
    private final ApplicationRepository applications;

    @Transactional(readOnly = true)
    public List<EmploymentRecordResponse> listOwn(UUID userId) {
        return records.findByUserIdOrderByStartDateDesc(userId).stream().map(record -> {
            var application = applications.findById(record.getApplicationId()).orElseThrow();
            return EmploymentRecordResponse.from(record, application.getCompanyName());
        }).toList();
    }
}
