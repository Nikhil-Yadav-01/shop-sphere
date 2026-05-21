package com.rudraksha.shopsphere.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
}
