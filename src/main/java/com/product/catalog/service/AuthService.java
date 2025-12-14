package com.product.catalog.service;

import com.product.catalog.dto.LoginRequest;
import com.product.catalog.dto.LoginResponse;

public interface AuthService {
    LoginResponse authenticate(LoginRequest request);
}

