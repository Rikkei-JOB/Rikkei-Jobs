package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CreateDataReportRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.DataReportView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.EvidenceAdminView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ResolveDataReportRequest;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.PageView;
import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.entity.Skill;
import com.rikkei.jobs.profile.entity.SkillEvidence;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.DataReport;
import com.rikkei.jobs.profile.entity.admin.DataReportStatus;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.repository.SkillEvidenceRepository;
import com.rikkei.jobs.profile.repository.SkillRepository;
import com.rikkei.jobs.profile.repository.admin.DataReportRepository;
import com.rikkei.jobs.profile.service.sync.CandidateDirectory;
import com.rikkei.jobs.profile.service.sync.CandidateDirectory.Candidate;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ứng viên báo một bằng chứng sai; ADMIN xem theo trạng thái và xử lý (RESOLVED hoặc REJECTED) kèm kết quả. */
@Service
public class DataReportService {
    private final DataReportRepository reports;
    private final SkillEvidenceRepository evidences;
    private final SkillRepository skills;
    private final CandidateDirectory directory;
    private final ChangeLogService changeLog;
    private final Clock clock;

    public DataReportService(DataReportRepository reports, SkillEvidenceRepository evidences, SkillRepository skills,
                             CandidateDirectory directory, ChangeLogService changeLog,
                             @Qualifier("skillProfileClock") Clock clock) {
        this.reports = reports;
        this.evidences = evidences;
        this.skills = skills;
        this.directory = directory;
        this.changeLog = changeLog;
        this.clock = clock;
    }

    @Transactional
    public DataReportView create(UUID userId, CreateDataReportRequest request) {
        // Bằng chứng của người khác trả 404 như khi không tồn tại (OwnershipGuard, NT-02).
        SkillEvidence evidence = evidences.findById(request.evidenceId())
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bằng chứng"));
        if (evidence.getSource() == EvidenceSource.SELF) {
            throw new BadRequestException("Kỹ năng tự khai do bạn tự sửa ở mục Kỹ năng tự đánh giá");
        }
        if (reports.existsByUserIdAndEvidenceIdAndStatus(userId, evidence.getId(), DataReportStatus.OPEN)) {
            throw new ConflictException("Bằng chứng này đang có báo cáo chờ xử lý");
        }
        Skill skill = skills.findById(evidence.getSkillId()).orElseThrow();
        DataReport report = new DataReport();
        report.setId(UUID.randomUUID());
        report.setUserId(userId);
        report.setEvidenceId(evidence.getId());
        report.setEvidenceTitle(evidence.getTitle());
        report.setSkillName(skill.getName());
        report.setContent(request.content().strip());
        report.setStatus(DataReportStatus.OPEN);
        report.setCreatedAt(clock.instant());
        return views(List.of(reports.save(report)), false).get(0);
    }

    @Transactional(readOnly = true)
    public List<DataReportView> mine(UUID userId) {
        return views(reports.findAllByUserIdOrderByCreatedAtDesc(userId), false);
    }

    @Transactional(readOnly = true)
    public PageView<DataReportView> list(DataReportStatus status, int page, int size) {
        if (page < 1) {
            throw new FieldValidationException("page", "page phải từ 1");
        }
        if (size < 1 || size > 100) {
            throw new FieldValidationException("size", "size phải từ 1 đến 100");
        }
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending().and(Sort.by("id")));
        Page<DataReport> result = status == null ? reports.findAll(pageable) : reports.findAllByStatus(status, pageable);
        return new PageView<>(views(result.getContent(), true), result.getTotalElements(), page, size);
    }

    @Transactional
    public DataReportView resolve(UUID id, ResolveDataReportRequest request, UUID adminId) {
        if (request.status() == DataReportStatus.OPEN) {
            throw new FieldValidationException("status", "Chỉ chọn RESOLVED hoặc REJECTED");
        }
        DataReport report = reports.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        if (report.getStatus() != DataReportStatus.OPEN) {
            throw new ConflictException("Báo cáo đã được xử lý");
        }
        String resolution = request.resolution().strip();
        changeLog.record(ChangeEntity.DATA_REPORT, id, "status", report.getStatus(), request.status(), resolution,
                adminId);
        report.setStatus(request.status());
        report.setResolution(resolution);
        report.setResolvedBy(adminId);
        report.setResolvedAt(clock.instant());
        return views(List.of(reports.save(report)), true).get(0);
    }

    /** withCandidate: kèm tên, email ứng viên và bằng chứng hiện tại cho màn ADMIN. */
    private List<DataReportView> views(Collection<DataReport> items, boolean withCandidate) {
        Map<UUID, Candidate> candidates = withCandidate
                ? directory.findAll(items.stream().map(DataReport::getUserId).distinct().toList())
                : Map.of();
        Map<UUID, EvidenceAdminView> currentEvidence = withCandidate ? currentEvidence(items) : Map.of();
        return items.stream().map(r -> {
            Candidate candidate = candidates.get(r.getUserId());
            return new DataReportView(r.getId(), r.getUserId(), candidate == null ? null : candidate.fullName(),
                    candidate == null ? null : candidate.email(), r.getEvidenceId(), r.getEvidenceTitle(),
                    r.getSkillName(), r.getContent(), r.getStatus(), r.getResolution(), r.getResolvedAt(),
                    r.getCreatedAt(), r.getEvidenceId() == null ? null : currentEvidence.get(r.getEvidenceId()));
        }).toList();
    }

    private Map<UUID, EvidenceAdminView> currentEvidence(Collection<DataReport> items) {
        List<SkillEvidence> rows = evidences.findAllById(items.stream().map(DataReport::getEvidenceId)
                .filter(Objects::nonNull).distinct().toList());
        Map<UUID, Skill> skillById = skills.findAllById(rows.stream().map(SkillEvidence::getSkillId).distinct().toList())
                .stream().collect(Collectors.toMap(Skill::getId, Function.identity()));
        return rows.stream().collect(Collectors.toMap(SkillEvidence::getId,
                e -> AdminRecordService.view(e, skillById.get(e.getSkillId()))));
    }
}
