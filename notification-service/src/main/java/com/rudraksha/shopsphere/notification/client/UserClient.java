package com.rudraksha.shopsphere.notification.client;

import com.rudraksha.shopsphere.notification.dto.UserDetailsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface UserClient {
    @GetMapping("/auth/users/{id}")
    UserDetailsResponse getUserById(@PathVariable("id") String id);
}
