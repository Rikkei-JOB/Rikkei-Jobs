package com.rikkei.jobs.identity.config;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Tài khoản mẫu theo docs/conventions.md (mục 4): 8 tài khoản @rikkeijobs.test, mật khẩu Test@123,
 * cộng thêm một HR "mới" để thử luồng bắt buộc đổi mật khẩu. Chạy lại không tạo bản ghi trùng.
 * Tài khoản LMS (CANDIDATE, MENTOR, STAFF, ADMIN) gắn sẵn lmsUserId khớp mock/data.json và đăng nhập bằng SSO;
 * chúng không có mật khẩu. Chỉ bật ở dev/test (app.dev-data.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    public static final String SAMPLE_PASSWORD = "Test@123";
    public static final UUID COMPANY_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final UUID COMPANY_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        lmsUser("candidate1@rikkeijobs.test", "Nguyễn Văn An", Role.CANDIDATE, "lms-sv-001");
        lmsUser("candidate2@rikkeijobs.test", "Trần Thị Bình", Role.CANDIDATE, "lms-sv-002");
        lmsUser("candidate3@rikkeijobs.test", "Lê Minh Châu", Role.CANDIDATE, "lms-sv-003");
        lmsUser("mentor1@rikkeijobs.test", "Phạm Quốc Dũng", Role.MENTOR, "lms-mt-001");
        lmsUser("staff1@rikkeijobs.test", "Hoàng Thu Hà", Role.STAFF, "lms-st-001");
        lmsUser("admin1@rikkeijobs.test", "Đỗ Thanh Hải", Role.ADMIN, "lms-ad-001");
        hr("hr1@rikkeijobs.test", "HR Công ty A", COMPANY_A_ID, false);
        hr("hr2@rikkeijobs.test", "HR Công ty B", COMPANY_B_ID, false);
        hr("hr-new@rikkeijobs.test", "HR Công ty A (tài khoản mới, phải đổi mật khẩu)", COMPANY_A_ID, true);
    }

    private void lmsUser(String email, String fullName, Role role, String lmsUserId) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setLmsUserId(lmsUserId);
        userRepository.save(user);
    }

    private void hr(String email, String fullName, UUID companyId, boolean mustChangePassword) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(Role.HR);
        user.setCompanyId(companyId);
        user.setPasswordHash(passwordEncoder.encode(SAMPLE_PASSWORD));
        user.setMustChangePassword(mustChangePassword);
        userRepository.save(user);
        log.info("Đã tạo HR mẫu {} (mật khẩu: {})", email, SAMPLE_PASSWORD);
    }
}
