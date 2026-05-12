package com.rudraksha.shopsphere.gateway.dto;

import java.util.List;

public class TokenValidationResponse {
    private boolean valid;
    private String userId;
    private List<String> roles;

    public TokenValidationResponse() {}

    public TokenValidationResponse(boolean valid, String userId, List<String> roles) {
        this.valid = valid;
        this.userId = userId;
        this.roles = roles;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
