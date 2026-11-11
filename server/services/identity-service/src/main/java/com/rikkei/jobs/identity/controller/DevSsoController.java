package com.rikkei.jobs.identity.controller;

import com.rikkei.jobs.identity.client.LmsClient;
import com.rikkei.jobs.identity.dto.request.DevSsoTicketRequest;
import com.rikkei.jobs.identity.dto.response.DevSsoTicketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chỉ dùng khi dev/demo (app.lms.dev-tickets.enabled=true): mô phỏng việc bấm nút "Đăng nhập Rikkei Jobs" trong LMS.
 * Frontend không được gọi thẳng mock (khoá dịch vụ chỉ nằm ở backend), nên backend cấp vé hộ qua mock rồi trả về
 * để trang đăng nhập chuyển sang /sso-callback?ticket=.
 */
@RestController
@RequestMapping("/auth/dev")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.lms.dev-tickets.enabled", havingValue = "true")
public class DevSsoController {

    private final LmsClient lmsClient;

    @PostMapping("/sso-ticket")
    public DevSsoTicketResponse issue(@Valid @RequestBody DevSsoTicketRequest request) {
        return lmsClient.issueTicket(request.getLmsUserId());
    }
}
