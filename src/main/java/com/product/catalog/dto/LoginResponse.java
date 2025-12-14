package com.product.catalog.dto;

/**
 * DTO for login response
 */
public class LoginResponse {

    private String token;

    private String type = "Bearer";

    private Long expiresIn;

    public LoginResponse() {
    }

    public LoginResponse(String token, String type, Long expiresIn) {
        this.token = token;
        this.type = type != null ? type : "Bearer";
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
