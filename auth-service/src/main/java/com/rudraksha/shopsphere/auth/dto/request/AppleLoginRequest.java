package com.rudraksha.shopsphere.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginRequest {
    @NotBlank(message = "Apple ID token is required")
    private String idToken;
    
    private String firstName;
    private String lastName;
}
