package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.ContactException;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
@Service @RequiredArgsConstructor
public class ContactRequestService {
    private final ContactRequestRepository requests;
    private final ContactAccessService access;
    private final ContactProfileClient profiles;
    private final JobApplicationClient jobs;
    private final ContactDirectoryClient directory;
    private final IdentityCandidateClient identities;
    private final Clock clock;

    static void requireHr(AuthPrincipal actor) {
        if(actor==null || actor.getRole()!=Role.HR || actor.getCompanyId()==null)
            throw new ForbiddenException("Chỉ HR thuộc công ty được truy cập liên hệ.");
    }
    static String reason(String value) {
        if(value==null || value.strip().isEmpty() || value.strip().length()>2000)
            throw new BadRequestException("Lý do cần từ 1 đến 2000 ký tự.");
        return value.strip();
    }
    @Transactional
    public ContactRequestView create(AuthPrincipal actor,UUID userId,UUID jobId,String reason,String bearer) {
        requireHr(actor);String text=reason(reason);
        if(access.canSeeContact(actor.getCompanyId(),userId))
            throw new ContactException(409,"CONTACT_ALREADY_ACCESSIBLE","Công ty đã có quyền xem liên hệ.");
        var profile=profiles.get(userId,bearer);
        if(!"PUBLIC".equals(profile.visibility()))throw new ResourceNotFoundException("Không tìm thấy ứng viên công khai.");
        if(jobId==null)throw new BadRequestException("Cần chọn JD.");
        var job=jobs.getContext(jobId);
        if(!actor.getCompanyId().equals(job.companyId()))throw new ResourceNotFoundException("Không tìm thấy JD của công ty.");
        if(requests.existsByCompanyIdAndUserIdAndStatus(actor.getCompanyId(),userId,ContactRequestStatus.PENDING))
            throw new ContactException(409,"CONTACT_REQUEST_PENDING","Yêu cầu đang chờ duyệt.");
        var request=new ContactRequest();request.setId(UUID.randomUUID());request.setCompanyId(actor.getCompanyId());
        request.setUserId(userId);request.setJobId(jobId);request.setReason(text);request.setStatus(ContactRequestStatus.PENDING);
        request.setCreatedBy(actor.getUserId());request.setCreatedAt(clock.instant());
        requests.saveAndFlush(request);
        return view(request,job.companyName(),profile.fullName());
    }
    @Transactional(readOnly=true)
    public ContactAccessView status(AuthPrincipal actor,UUID userId,String bearer) {
        requireHr(actor);
        var via=access.accessVia(actor.getCompanyId(),userId);
        var profile=profiles.get(userId,bearer);
        if(via.isEmpty() && !"PUBLIC".equals(profile.visibility()))throw new ResourceNotFoundException("Không tìm thấy ứng viên công khai.");
        var latest=requests.findFirstByCompanyIdAndUserIdOrderByCreatedAtDescIdDesc(actor.getCompanyId(),userId).orElse(null);
        return new ContactAccessView(userId,profile.fullName(),via.isPresent(),via.orElse(null),
            latest==null?null:latest.getStatus(),latest==null?null:view(latest,null,profile.fullName()));
    }
    @Transactional(readOnly=true)
    public PageResponse<ContactRequestView> list(ContactRequestStatus status,int page,int size) {
        InterviewQueryService.page(page,size);
        var result=requests.findFiltered(status,PageRequest.of(page-1,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
        if(result.isEmpty())return PageResponse.of(result,List.of());
        var companies=new HashSet<UUID>();var users=new ArrayList<UUID>();
        result.forEach(r->{companies.add(r.getCompanyId());if(!users.contains(r.getUserId()))users.add(r.getUserId());});
        var names=directory.lookup(companies,Set.of()).companies();
        var candidates=identities.getBatch(users);
        return PageResponse.of(result,result.stream().map(r->view(r,names.get(r.getCompanyId()),
            candidates.containsKey(r.getUserId())?candidates.get(r.getUserId()).fullName():r.getUserId().toString())).toList());
    }
    @Transactional
    public ContactRequestView review(AuthPrincipal staff,UUID id,boolean approved,String reason) {
        if(staff==null || staff.getRole()!=Role.STAFF)throw new ForbiddenException("Chỉ STAFF được duyệt.");
        String rejection=approved?null:reason(reason);
        var r=requests.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy yêu cầu."));
        if(r.getStatus()!=ContactRequestStatus.PENDING)throw new ContactException(409,"CONTACT_REQUEST_ALREADY_REVIEWED","Yêu cầu đã được xử lý.");
        r.setStatus(approved?ContactRequestStatus.APPROVED:ContactRequestStatus.REJECTED);
        r.setReviewedBy(staff.getUserId());r.setReviewedAt(clock.instant());r.setRejectionReason(rejection);
        return view(r,null,null);
    }
    static ContactRequestView view(ContactRequest r,String companyName,String candidateName) {
        return new ContactRequestView(r.getId(),r.getCompanyId(),companyName,r.getUserId(),candidateName,r.getJobId(),
            r.getReason(),r.getStatus(),r.getRejectionReason(),r.getCreatedBy(),r.getCreatedAt(),r.getReviewedBy(),r.getReviewedAt());
    }
}
