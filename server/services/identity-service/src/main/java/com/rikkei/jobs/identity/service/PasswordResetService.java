package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.dto.request.ForgotPasswordRequest;
import com.rikkei.jobs.identity.dto.request.ResetPasswordRequest;

public interface PasswordResetService {
    /** Luôn hoàn tất êm lặng để người gọi không biết email có tồn tại hay không. */
    void requestReset(ForgotPasswordRequest request);

    void reset(ResetPasswordRequest request);
}
