package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.*;
import com.rikkei.jobs.profile.entity.Certificate;
import com.rikkei.jobs.profile.entity.Education;
import com.rikkei.jobs.profile.repository.CertificateRepository;
import com.rikkei.jobs.profile.repository.EducationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ProfileRecordsService {
    private final EducationRepository educations;
    private final CertificateRepository certificates;
    private final ProfileFileService files;

    public List<EducationResponse> educations(UUID owner) {
        return educations.findAllByUserIdOrderByFromDateDescIdAsc(owner).stream().map(EducationResponse::from).toList();
    }
    @Transactional
    public EducationResponse createEducation(UUID owner, EducationRequest request) {
        var e = new Education(); e.setUserId(owner);
        apply(e, request);
        return EducationResponse.from(educations.save(e));
    }
    @Transactional
    public EducationResponse updateEducation(UUID owner, UUID id, EducationRequest request) {
        var e = education(owner, id); apply(e, request);
        return EducationResponse.from(educations.save(e));
    }
    @Transactional
    public void deleteEducation(UUID owner, UUID id) { educations.delete(education(owner, id)); }
    private Education education(UUID owner, UUID id) {
        return educations.findByIdAndUserId(id, owner).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học vấn"));
    }
    private static void apply(Education e, EducationRequest r) {
        e.setSchool(r.school()); e.setMajor(r.major()); e.setFromDate(r.fromDate());
        e.setToDate(r.toDate()); e.setGpa(r.gpa()); e.setNote(r.note());
    }
    public List<CertificateResponse> certificates(UUID owner) {
        return certificates.findAllByUserIdOrderByIssuedAtDescIdAsc(owner).stream().map(CertificateResponse::from).toList();
    }
    @Transactional
    public CertificateResponse createCertificate(UUID owner, CertificateRequest request) {
        var c = new Certificate(); c.setUserId(owner); apply(c, request, owner);
        return CertificateResponse.from(certificates.save(c));
    }
    @Transactional
    public CertificateResponse updateCertificate(UUID owner, UUID id, CertificateRequest request) {
        var c = certificate(owner, id); apply(c, request, owner);
        return CertificateResponse.from(certificates.save(c));
    }
    @Transactional
    public void deleteCertificate(UUID owner, UUID id) { certificates.delete(certificate(owner, id)); }
    private Certificate certificate(UUID owner, UUID id) {
        return certificates.findByIdAndUserId(id, owner).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
    }
    private void apply(Certificate c, CertificateRequest r, UUID owner) {
        String type = r.fileUrl() == null ? null : files.requireOwned(owner, r.fileUrl()).getContentType();
        c.setName(r.name()); c.setIssuer(r.issuer()); c.setIssuedAt(r.issuedAt()); c.setExpiresAt(r.expiresAt());
        c.setFileUrl(r.fileUrl()); c.setFileType(type);
    }
}
