package com.rudraksha.shopsphere.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialUserInfo {
    private String email;
    private String firstName;
    private String lastName;
    private String providerId;
}
