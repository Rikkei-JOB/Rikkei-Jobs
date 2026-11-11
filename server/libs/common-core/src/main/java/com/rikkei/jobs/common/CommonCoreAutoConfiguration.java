package com.rikkei.jobs.common;

import com.rikkei.jobs.common.config.exception.GlobalExceptionHandler;
import com.rikkei.jobs.common.config.security.config.SecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

/** Đăng ký xử lý lỗi chung và bảo mật JWT cho mọi service có dependency common-core. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class CommonCoreAutoConfiguration {
}
