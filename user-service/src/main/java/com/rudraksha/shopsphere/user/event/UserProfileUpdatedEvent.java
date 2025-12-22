package com.rudraksha.shopsphere.user.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdatedEvent {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("profile_id")
    private UUID profileId;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
