package com.rikkei.jobs.recruitment.dto.contact;
import com.rikkei.jobs.recruitment.entity.*;
import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;
public final class ContactDtos {
    private ContactDtos() {}
    public record ContactProfile(UUID userId,String fullName,String visibility) {}
    public record ContactDirectory(Map<UUID,String> companies,Map<UUID,String> hrs) {}
    public record CreateRequest(@NotNull UUID jobId,String reason) {}
    public record RejectRequest(String reason) {}
    public record ContactView(String email,String phone) {}
    public record ContactRequestView(UUID id,UUID companyId,String companyName,UUID userId,String candidateName,
        UUID jobId,String reason,ContactRequestStatus status,String rejectionReason,UUID createdBy,
        Instant createdAt,UUID reviewedBy,Instant reviewedAt) {}
    public record ContactAccessView(UUID userId,String fullName,boolean canSeeContact,ContactAccessVia via,
        ContactRequestStatus requestStatus,ContactRequestView latestRequest) {}
    public record ContactAccessLogView(UUID id,UUID companyId,String companyName,UUID hrId,String hrName,
        UUID userId,String candidateName,ContactAccessVia via,Instant createdAt) {}
}
