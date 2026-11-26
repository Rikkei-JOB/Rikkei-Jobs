package com.rikkei.jobs.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.CvDtos.*;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.entity.*;
import com.rikkei.jobs.profile.repository.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** HS-04: CV theo vị trí. CV chỉ lưu phần/mục được chọn, nội dung đọc từ hồ sơ tại thời điểm xem hoặc xuất. */
@Service
@Transactional(readOnly = true)
public class CvService {
    public static final int MAX_CVS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final TypeReference<List<UUID>> ID_LIST = new TypeReference<>() { };

    private final CvRepository cvs;
    private final CvSectionRepository sections;
    private final CandidateProfileRepository profiles;
    private final SkillProfileService skills;
    private final EducationRepository educations;
    private final CertificateRepository certificates;
    private final LanguageRepository languages;
    private final PortfolioProjectRepository projects;
    private final SkillProfileStateRepository states;
    private final ObjectMapper json;
    private final Clock clock;
    private final String webBaseUrl;

    public CvService(CvRepository cvs, CvSectionRepository sections, CandidateProfileRepository profiles,
        SkillProfileService skills, EducationRepository educations, CertificateRepository certificates, LanguageRepository languages,
        PortfolioProjectRepository projects, SkillProfileStateRepository states, ObjectMapper json,
        @Qualifier("skillProfileClock") Clock clock, @Value("${app.profile.web-base-url}") String webBaseUrl) {
        this.cvs = cvs; this.sections = sections; this.profiles = profiles; this.skills = skills;
        this.educations = educations; this.certificates = certificates; this.languages = languages; this.projects = projects;
        this.states = states; this.json = json; this.clock = clock;
        this.webBaseUrl = webBaseUrl.endsWith("/") ? webBaseUrl.substring(0, webBaseUrl.length() - 1) : webBaseUrl;
    }

    /** Nội dung đã lọc để dựng PDF: chỉ phần được bật, chỉ mục được chọn, bỏ phần rỗng. */
    public record Rendered(Cv cv, CandidateProfile profile, String publicUrl, List<SectionView> sections) { }

    public List<CvSummary> list(UUID owner) {
        return cvs.findAllByUserIdOrderByCreatedAtAscIdAsc(owner).stream()
            .map(cv -> new CvSummary(cv.getId(), cv.getName(), cv.getTargetPosition(), cv.getTemplate(),
                publicUrl(cv), cv.getCreatedAt(), cv.getUpdatedAt())).toList();
    }

    public CvDetail get(UUID owner, UUID id) { return detail(cv(owner, id)); }

    @Transactional
    public CvDetail create(UUID owner, CvRequest request) {
        // Khoá theo ứng viên để hai request đồng thời không vượt giới hạn số CV.
        states.ensureExists(owner);
        states.lockByUserId(owner).orElseThrow();
        if (cvs.countByUserId(owner) >= MAX_CVS) {
            throw new ProfileFileException(409, "CV_LIMIT_REACHED", "Mỗi ứng viên tối đa " + MAX_CVS + " CV.");
        }
        var now = clock.instant();
        var cv = new Cv();
        cv.setUserId(owner); cv.setPublicToken(newToken());
        cv.setCreatedAt(now); cv.setUpdatedAt(now);
        apply(cv, request);
        cv = cvs.save(cv);
        int order = 0;
        List<CvSection> defaults = new ArrayList<>();
        for (var type : CvSection.Type.values()) defaults.add(section(cv.getId(), type, order++, true, null));
        sections.saveAll(defaults);
        return detail(cv);
    }

    @Transactional
    public CvDetail update(UUID owner, UUID id, CvRequest request) {
        var cv = cv(owner, id);
        apply(cv, request);
        cv.setUpdatedAt(clock.instant());
        return detail(cvs.save(cv));
    }

    @Transactional
    public void delete(UUID owner, UUID id) { cvs.delete(cv(owner, id)); }

    @Transactional
    public CvDetail replaceSections(UUID owner, UUID id, SectionsRequest request) {
        var cv = cv(owner, id);
        validate(request);
        Map<CvSection.Type, CvSection> existing = sections.findAllByCvIdOrderBySortOrderAsc(cv.getId()).stream()
            .collect(Collectors.toMap(CvSection::getSection, Function.identity()));
        List<CvSection> rows = new ArrayList<>();
        for (int i = 0; i < request.sections().size(); i++) {
            var r = request.sections().get(i);
            // Cập nhật tại chỗ: xoá rồi chèn lại cùng khoá chính sẽ đụng thứ tự flush của Hibernate.
            var row = existing.getOrDefault(r.section(), section(cv.getId(), r.section(), i, r.included(), null));
            row.setSortOrder(i); row.setIncluded(r.included());
            row.setItemIds(itemIds(r));
            rows.add(row);
        }
        sections.saveAll(rows);
        cv.setUpdatedAt(clock.instant());
        return detail(cvs.save(cv));
    }

    public Rendered render(UUID owner, UUID id) { return render(owner, id, null); }

    /** draft != null: dựng theo bản nháp chưa lưu (xem trước trực tiếp), không ghi gì xuống DB. */
    public Rendered render(UUID owner, UUID id, SectionsRequest draft) {
        var cv = cv(owner, id);
        var profile = profiles.findById(owner).orElseThrow(() -> new ProfileFileException(409, "PROFILE_REQUIRED",
            "Hãy hoàn thiện thông tin cá nhân trước khi xuất CV."));
        List<CvSection> rows;
        if (draft == null) rows = sections.findAllByCvIdOrderBySortOrderAsc(cv.getId());
        else {
            validate(draft);
            rows = new ArrayList<>();
            for (int i = 0; i < draft.sections().size(); i++) {
                var r = draft.sections().get(i);
                rows.add(section(cv.getId(), r.section(), i, r.included(), itemIds(r)));
            }
        }
        var visible = buildSections(cv, profile, rows).stream().filter(SectionView::included)
            .map(s -> new SectionView(s.section(), s.label(), true, s.allItems(), s.text(),
                s.items().stream().filter(ItemView::selected).toList()))
            .filter(s -> s.section() == CvSection.Type.SUMMARY ? s.text() != null && !s.text().isBlank() : !s.items().isEmpty())
            .toList();
        return new Rendered(cv, profile, publicUrl(cv), visible);
    }

    /** QR dẫn về đây. Không lộ liên hệ; hồ sơ không PUBLIC thì chỉ trả visible = false. */
    public PublicProfile publicProfile(String token) {
        var cv = cvs.findByPublicToken(token).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
        UUID owner = cv.getUserId();
        if (!profiles.isPublic(owner)) return PublicProfile.hidden();
        var profile = profiles.findById(owner).orElse(null);
        if (profile == null) return PublicProfile.hidden();
        var verified = skills.getSkills(owner).stream().filter(s -> s.verifiedScore() != null)
            .map(s -> new PublicSkill(s.skill().name(), s.skill().group(), levelLabel(s.level()),
                s.verifiedScore().setScale(0, java.math.RoundingMode.HALF_UP))).toList();
        var internships = projects.findAllByUserIdAndVisibleTrueOrderBySortOrderAscIdAsc(owner).stream()
            .filter(PortfolioProject::isIoc)
            .map(p -> new PublicInternship(p.getName(), p.getRole(), tags(p), period(p.getStartDate(), p.getEndDate()), p.getDescription()))
            .toList();
        return new PublicProfile(true, profile.getFullName(), profile.getHeadline(), profile.getProgram(),
            profile.getCourseName(), verified, internships);
    }

    private CvDetail detail(Cv cv) {
        var profile = profiles.findById(cv.getUserId()).orElse(null);
        return new CvDetail(cv.getId(), cv.getName(), cv.getTargetPosition(), cv.getTemplate(), publicUrl(cv),
            cv.getCreatedAt(), cv.getUpdatedAt(), buildSections(cv, profile, sections.findAllByCvIdOrderBySortOrderAsc(cv.getId())));
    }

    private static void validate(SectionsRequest request) {
        var types = request.sections().stream().map(SectionRequest::section).toList();
        if (types.size() != CvSection.Type.values().length || !EnumSet.copyOf(types).equals(EnumSet.allOf(CvSection.Type.class))) {
            throw new BadRequestException("Danh sách phần phải gồm đúng mỗi phần một lần: " + Arrays.toString(CvSection.Type.values()));
        }
    }

    private String itemIds(SectionRequest r) {
        return r.section() == CvSection.Type.SUMMARY || r.itemIds() == null ? null : write(new LinkedHashSet<>(r.itemIds()));
    }

    private List<SectionView> buildSections(Cv cv, CandidateProfile profile, List<CvSection> rows) {
        UUID owner = cv.getUserId();
        List<ItemView> skillItems = skills.getSkills(owner).stream()
            .filter(s -> s.verifiedScore() != null || s.selfScore() != null).map(CvService::skillItem).toList();
        var allProjects = projects.findAllByUserIdOrderBySortOrderAscIdAsc(owner);
        Map<CvSection.Type, List<ItemView>> items = new EnumMap<>(CvSection.Type.class);
        items.put(CvSection.Type.SKILLS, skillItems);
        items.put(CvSection.Type.INTERNSHIP, allProjects.stream().filter(PortfolioProject::isIoc).map(p -> projectItem(p, true)).toList());
        items.put(CvSection.Type.PROJECTS, allProjects.stream().filter(p -> !p.isIoc()).map(p -> projectItem(p, false)).toList());
        items.put(CvSection.Type.EDUCATION, educations.findAllByUserIdOrderByFromDateDescIdAsc(owner).stream().map(CvService::educationItem).toList());
        items.put(CvSection.Type.CERTIFICATES, certificates.findAllByUserIdOrderByIssuedAtDescIdAsc(owner).stream().map(CvService::certificateItem).toList());
        items.put(CvSection.Type.LANGUAGES, languages.findAllByUserIdOrderByLevelDescNameAsc(owner).stream().map(CvService::languageItem).toList());
        items.put(CvSection.Type.SUMMARY, List.of());
        String summary = profile == null ? null : firstNonBlank(profile.getSummary(), profile.getHeadline());

        return rows.stream().map(s -> {
            Set<UUID> chosen = s.getItemIds() == null ? null : new HashSet<>(read(s.getItemIds()));
            var marked = items.get(s.getSection()).stream()
                .map(i -> new ItemView(i.id(), i.title(), i.subtitle(), i.period(), i.detail(), i.verified(),
                    chosen == null || chosen.contains(i.id()))).toList();
            return new SectionView(s.getSection(), label(s.getSection()), s.isIncluded(), chosen == null,
                s.getSection() == CvSection.Type.SUMMARY ? summary : null, marked);
        }).toList();
    }

    private static ItemView skillItem(SkillProfileView s) {
        boolean verified = s.verifiedScore() != null;
        String detail = verified
            ? "Đã xác thực · " + points(s.verifiedScore()) + " điểm" + (s.level() == null ? "" : " · " + levelLabel(s.level()))
            : "Tự khai · " + points(s.selfScore()) + " điểm";
        return new ItemView(s.skill().id(), s.skill().name(), s.skill().group(), null, detail, verified, true);
    }

    private static ItemView projectItem(PortfolioProject p, boolean verified) {
        var parts = new ArrayList<String>();
        if (!tags(p).isEmpty()) parts.add("Công nghệ: " + String.join(", ", tags(p)));
        if (p.getDescription() != null && !p.getDescription().isBlank()) parts.add(p.getDescription().strip());
        return new ItemView(p.getId(), p.getName(), p.getRole(), period(p.getStartDate(), p.getEndDate()),
            parts.isEmpty() ? null : String.join("\n", parts), verified, true);
    }

    private static ItemView educationItem(Education e) {
        var parts = new ArrayList<String>();
        if (e.getGpa() != null) parts.add("GPA: " + score(e.getGpa()));
        if (e.getNote() != null && !e.getNote().isBlank()) parts.add(e.getNote().strip());
        return new ItemView(e.getId(), e.getSchool(), e.getMajor(), period(e.getFromDate(), e.getToDate()),
            parts.isEmpty() ? null : String.join("\n", parts), null, true);
    }

    private static ItemView languageItem(Language l) {
        return new ItemView(l.getId(), l.getName(), "CEFR " + l.getLevel(), null, l.getNote(), null, true);
    }

    private static ItemView certificateItem(Certificate c) {
        String period = "Cấp " + c.getIssuedAt().format(MONTH) + (c.getExpiresAt() == null ? "" : " · Hết hạn " + c.getExpiresAt().format(MONTH));
        return new ItemView(c.getId(), c.getName(), c.getIssuer(), period, null, null, true);
    }

    static String label(CvSection.Type type) {
        return switch (type) {
            case SUMMARY -> "Giới thiệu";
            case SKILLS -> "Kỹ năng";
            case LANGUAGES -> "Ngoại ngữ";
            case INTERNSHIP -> "Thực tập";
            case PROJECTS -> "Dự án";
            case EDUCATION -> "Học vấn";
            case CERTIFICATES -> "Chứng chỉ";
        };
    }

    static String levelLabel(SkillLevel level) {
        if (level == null) return null;
        return switch (level) { case BASIC -> "Cơ bản"; case FAIR -> "Khá"; case GOOD -> "Tốt"; case EXCELLENT -> "Xuất sắc"; };
    }

    private static List<String> tags(PortfolioProject p) {
        return p.getTechStack() == null || p.getTechStack().isBlank() ? List.of()
            : Arrays.stream(p.getTechStack().split(",")).map(String::strip).filter(t -> !t.isEmpty()).toList();
    }

    private static String period(LocalDate from, LocalDate to) {
        if (from == null && to == null) return null;
        return (from == null ? "?" : from.format(MONTH)) + " – " + (to == null ? "nay" : to.format(MONTH));
    }

    private static String score(BigDecimal value) { return value == null ? "?" : value.stripTrailingZeros().toPlainString(); }

    /** Điểm kỹ năng là trung bình có trọng số (nhiều chữ số lẻ); CV và trang công khai hiện số nguyên. */
    private static String points(BigDecimal value) { return value == null ? "?" : value.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString(); }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.strip();
        return b == null || b.isBlank() ? null : b.strip();
    }

    private String publicUrl(Cv cv) { return webBaseUrl + "/p/" + cv.getPublicToken(); }

    private Cv cv(UUID owner, UUID id) {
        return cvs.findByIdAndUserId(id, owner).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CV"));
    }

    private static void apply(Cv cv, CvRequest r) {
        cv.setName(r.name().strip());
        cv.setTargetPosition(r.targetPosition() == null || r.targetPosition().isBlank() ? null : r.targetPosition().strip());
    }

    private static CvSection section(UUID cvId, CvSection.Type type, int order, boolean included, String itemIds) {
        var s = new CvSection();
        s.setCvId(cvId); s.setSection(type); s.setSortOrder(order); s.setIncluded(included); s.setItemIds(itemIds);
        return s;
    }

    private static String newToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String write(Collection<UUID> ids) {
        try { return json.writeValueAsString(ids); }
        catch (JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }

    private List<UUID> read(String ids) {
        try { return json.readValue(ids, ID_LIST); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Corrupt cv_sections.item_ids", ex); }
    }
}
