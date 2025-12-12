package com.rudraksha.shopsphere.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;

    @JsonProperty("auth_user_id")
    private UUID authUserId;

    private String phone;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private List<AddressResponse> addresses;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
