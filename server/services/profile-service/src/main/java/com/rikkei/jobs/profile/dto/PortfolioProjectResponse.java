package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.PortfolioMedia;
import com.rikkei.jobs.profile.entity.PortfolioProject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** {@code verified} = dự án IOC (dữ liệu từ thực tập, đã xác thực). */
public record PortfolioProjectResponse(UUID id, String source, boolean verified, UUID internshipId, String name,
    String role, String description, List<String> techStack, String repoUrl, String demoUrl,
    LocalDate startDate, LocalDate endDate, boolean visible, int sortOrder, List<Media> media) {

    /** IMAGE: {@code url} là đường dẫn đọc ảnh qua API (cần JWT); EVIDENCE: link minh chứng gốc. */
    public record Media(UUID id, String type, String url, String caption) {
        static Media from(UUID ownerId, PortfolioMedia m) {
            String url = PortfolioMedia.IMAGE.equals(m.getType())
                ? "/api/v1/profiles/" + ownerId + "/portfolio/media/" + m.getId() : m.getUrl();
            return new Media(m.getId(), m.getType(), url, m.getCaption());
        }
    }

    public static PortfolioProjectResponse from(PortfolioProject p, List<PortfolioMedia> media) {
        List<String> tags = p.getTechStack() == null || p.getTechStack().isEmpty() ? List.of() : Arrays.asList(p.getTechStack().split(","));
        return new PortfolioProjectResponse(p.getId(), p.getSource(), p.isIoc(), p.getInternshipId(), p.getName(),
            p.getRole(), p.getDescription(), tags, p.getRepoUrl(), p.getDemoUrl(), p.getStartDate(), p.getEndDate(),
            p.isVisible(), p.getSortOrder(), media.stream().map(m -> Media.from(p.getUserId(), m)).toList());
    }
}
