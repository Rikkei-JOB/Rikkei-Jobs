package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.PortfolioProjectRequest;
import com.rikkei.jobs.profile.dto.PortfolioProjectResponse;
import com.rikkei.jobs.profile.entity.PortfolioMedia;
import com.rikkei.jobs.profile.entity.PortfolioProject;
import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import com.rikkei.jobs.profile.repository.PortfolioMediaRepository;
import com.rikkei.jobs.profile.repository.PortfolioProjectRepository;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class PortfolioService {
    public static final int MAX_IMAGES = 6;
    private final PortfolioProjectRepository projects;
    private final PortfolioMediaRepository media;
    private final CandidateProfileRepository profiles;
    private final ProfileFileService files;
    private final Clock clock;

    public PortfolioService(PortfolioProjectRepository projects, PortfolioMediaRepository media,
        CandidateProfileRepository profiles, ProfileFileService files, @Qualifier("skillProfileClock") Clock clock) {
        this.projects = projects; this.media = media; this.profiles = profiles; this.files = files; this.clock = clock;
    }

    public List<PortfolioProjectResponse> mine(UUID owner) {
        return withMedia(projects.findAllByUserIdOrderBySortOrderAscIdAsc(owner));
    }

    /** Người khác chỉ xem được khi hồ sơ PUBLIC; chỉ trả dự án visible. Không lộ sự tồn tại của hồ sơ ẩn (404). */
    public List<PortfolioProjectResponse> ofUser(UUID ownerId, UUID viewerId) {
        requireViewable(ownerId, viewerId);
        return withMedia(projects.findAllByUserIdAndVisibleTrueOrderBySortOrderAscIdAsc(ownerId));
    }

    @Transactional
    public PortfolioProjectResponse create(UUID owner, PortfolioProjectRequest request) {
        var p = new PortfolioProject();
        p.setUserId(owner); p.setSource(PortfolioProject.PERSONAL);
        p.setSortOrder(projects.maxSortOrder(owner) + 1);
        applyAll(p, request);
        return PortfolioProjectResponse.from(projects.save(p), List.of());
    }

    @Transactional
    public PortfolioProjectResponse update(UUID owner, UUID id, PortfolioProjectRequest request) {
        var p = project(owner, id);
        if (p.isIoc()) {
            // Dữ liệu IOC đã xác thực: ứng viên chỉ được trình bày lại vai trò, mô tả và ẩn/hiện.
            p.setRole(request.role()); p.setDescription(request.description());
            if (request.visible() != null) p.setVisible(request.visible());
        } else {
            applyAll(p, request);
        }
        return withMedia(List.of(projects.save(p))).getFirst();
    }

    @Transactional
    public void delete(UUID owner, UUID id) {
        var p = project(owner, id);
        if (p.isIoc()) throw locked();
        // shortcut: tệp ảnh trên đĩa không bị xoá cùng dự án, dọn bằng job đối soát lưu trữ nếu cần.
        projects.delete(p);
    }

    @Transactional
    public List<PortfolioProjectResponse> reorder(UUID owner, List<UUID> ids) {
        var all = projects.findAllByUserIdOrderBySortOrderAscIdAsc(owner);
        Map<UUID, PortfolioProject> byId = all.stream().collect(Collectors.toMap(PortfolioProject::getId, p -> p));
        if (ids == null || ids.size() != all.size() || !byId.keySet().equals(new HashSet<>(ids))) {
            throw new BadRequestException("Danh sách thứ tự phải gồm đúng toàn bộ dự án của bạn, không trùng lặp.");
        }
        for (int i = 0; i < ids.size(); i++) byId.get(ids.get(i)).setSortOrder(i);
        projects.saveAll(all);
        return withMedia(ids.stream().map(byId::get).toList());
    }

    @Transactional
    public PortfolioProjectResponse.Media addImage(UUID owner, UUID projectId, MultipartFile file, String caption) {
        var p = projects.lockByIdAndUserId(projectId, owner).orElseThrow(PortfolioService::notFound);
        if (media.countByProjectIdAndType(p.getId(), PortfolioMedia.IMAGE) >= MAX_IMAGES) {
            throw new ProfileFileException(400, "TOO_MANY_IMAGES", "Mỗi dự án tối đa " + MAX_IMAGES + " ảnh.");
        }
        var m = new PortfolioMedia();
        m.setProjectId(p.getId()); m.setType(PortfolioMedia.IMAGE);
        m.setUrl(files.uploadImage(owner, file));
        m.setCaption(caption == null || caption.isBlank() ? null : caption.strip());
        m.setCreatedAt(clock.instant());
        return PortfolioProjectResponse.from(p, List.of(media.save(m))).media().getFirst();
    }

    @Transactional
    public void deleteMedia(UUID owner, UUID projectId, UUID mediaId) {
        var p = project(owner, projectId);
        var m = media.findById(mediaId).filter(x -> x.getProjectId().equals(p.getId())).orElseThrow(PortfolioService::notFound);
        if (!PortfolioMedia.IMAGE.equals(m.getType())) throw locked();
        media.delete(m);
    }

    public ProfileFileService.Download image(UUID ownerId, UUID viewerId, UUID mediaId) {
        boolean self = ownerId.equals(viewerId);
        if (!self) requireViewable(ownerId, viewerId);
        var m = media.findById(mediaId).filter(x -> PortfolioMedia.IMAGE.equals(x.getType())).orElseThrow(PortfolioService::notFound);
        var p = projects.findByIdAndUserId(m.getProjectId(), ownerId).orElseThrow(PortfolioService::notFound);
        if (!self && !p.isVisible()) throw notFound();
        return files.download(ownerId, files.requireOwned(ownerId, m.getUrl()).getId());
    }

    private void requireViewable(UUID ownerId, UUID viewerId) {
        if (viewerId == null || (!ownerId.equals(viewerId) && !profiles.isPublic(ownerId))) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ");
        }
    }

    private List<PortfolioProjectResponse> withMedia(List<PortfolioProject> list) {
        if (list.isEmpty()) return List.of();
        Map<UUID, List<PortfolioMedia>> byProject = media.findAllByProjectIdInOrderByCreatedAtAscIdAsc(
            list.stream().map(PortfolioProject::getId).toList()).stream().collect(Collectors.groupingBy(PortfolioMedia::getProjectId));
        return list.stream().map(p -> PortfolioProjectResponse.from(p, byProject.getOrDefault(p.getId(), List.of()))).toList();
    }

    private PortfolioProject project(UUID owner, UUID id) {
        return projects.findByIdAndUserId(id, owner).orElseThrow(PortfolioService::notFound);
    }

    private static void applyAll(PortfolioProject p, PortfolioProjectRequest r) {
        p.setName(r.name()); p.setRole(r.role()); p.setDescription(r.description());
        p.setTechStack(r.techStack().isEmpty() ? null : String.join(",", r.techStack()));
        p.setRepoUrl(r.repoUrl()); p.setDemoUrl(r.demoUrl());
        p.setStartDate(r.startDate()); p.setEndDate(r.endDate());
        p.setVisible(r.visible() == null || r.visible());
    }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Không tìm thấy dự án"); }
    private static ProfileFileException locked() {
        return new ProfileFileException(409, "IOC_PROJECT_LOCKED", "Dữ liệu dự án thực tập IOC đã xác thực, không thể xoá.");
    }
}
