package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.*;
import com.rikkei.jobs.recruitment.entity.ContactAccessLog;
import com.rikkei.jobs.recruitment.repository.ContactAccessLogRepository;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ContactAccessLogService {
    private final ContactAccessLogRepository logs;
    private final ContactDirectoryClient directory;
    private final IdentityCandidateClient identities;
    public PageResponse<ContactAccessLogView> myLogs(AuthPrincipal actor,int page,int size) {
        if(actor==null || actor.getRole()!=Role.CANDIDATE)throw new ForbiddenException("Chỉ ứng viên được xem lịch sử của mình.");
        return query(null,actor.getUserId(),null,null,page,size,false);
    }
    public PageResponse<ContactAccessLogView> staffLogs(UUID companyId,UUID userId,Instant from,Instant to,int page,int size) {
        return query(companyId,userId,from,to,page,size,true);
    }
    private PageResponse<ContactAccessLogView> query(UUID companyId,UUID userId,Instant from,Instant to,int page,int size,boolean staff) {
        InterviewQueryService.page(page,size);
        if(from!=null && to!=null && !from.isBefore(to))throw new BadRequestException("Khoảng thời gian không hợp lệ.");
        var result=logs.findAll((root,q,cb)->{
            var filters=new ArrayList<Predicate>();
            if(companyId!=null)filters.add(cb.equal(root.get("companyId"),companyId));
            if(userId!=null)filters.add(cb.equal(root.get("userId"),userId));
            if(from!=null)filters.add(cb.greaterThanOrEqualTo(root.get("createdAt"),from));
            if(to!=null)filters.add(cb.lessThan(root.get("createdAt"),to));
            return cb.and(filters.toArray(Predicate[]::new));
        },PageRequest.of(page-1,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
        if(result.isEmpty())return PageResponse.of(result,List.of());
        var companies=result.stream().map(ContactAccessLog::getCompanyId).collect(Collectors.toSet());
        var hrs=staff?result.stream().map(ContactAccessLog::getHrId).collect(Collectors.toSet()):Set.<UUID>of();
        ContactDirectory names;
        try{names=directory.lookup(companies,hrs);}catch(RecruitmentDependencyException e){names=new ContactDirectory(Map.of(),Map.of());}
        Map<UUID,CandidateIdentity> candidates=Map.of();
        if(staff)try{candidates=identities.getBatch(result.stream().map(ContactAccessLog::getUserId).distinct().toList());}
            catch(RecruitmentDependencyException e){/* Stable IDs still identify the immutable audit rows. */}
        var d=names;var c=candidates;
        return PageResponse.of(result,result.stream().map(l->new ContactAccessLogView(l.getId(),l.getCompanyId(),
            d.companies().getOrDefault(l.getCompanyId(),l.getCompanyId().toString()),l.getHrId(),
            staff?d.hrs().getOrDefault(l.getHrId(),l.getHrId().toString()):null,l.getUserId(),
            c.containsKey(l.getUserId())?c.get(l.getUserId()).fullName():null,l.getVia(),l.getCreatedAt())).toList());
    }
}
