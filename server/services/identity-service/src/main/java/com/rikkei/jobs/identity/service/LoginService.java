package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.entity.User;

public interface LoginService {
    /** Đăng nhập email + mật khẩu, kèm khoá tài khoản khi sai nhiều lần liên tiếp. */
    User authenticate(String email, String password);
}
