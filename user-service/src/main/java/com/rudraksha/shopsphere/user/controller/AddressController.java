package com.rudraksha.shopsphere.user.controller;

import com.rudraksha.shopsphere.user.dto.request.CreateAddressRequest;
import com.rudraksha.shopsphere.user.dto.response.AddressResponse;
import com.rudraksha.shopsphere.user.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getUserAddresses(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/addresses - Fetching all addresses", userId);
        List<AddressResponse> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        log.info("GET /api/v1/users/{}/addresses/{} - Fetching address", userId, addressId);
        AddressResponse address = addressService.getAddress(userId, addressId);
        return ResponseEntity.ok(address);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAddressRequest request) {
        log.info("POST /api/v1/users/{}/addresses - Creating address", userId);
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId,
            @Valid @RequestBody CreateAddressRequest request) {
        log.info("PUT /api/v1/users/{}/addresses/{} - Updating address", userId, addressId);
        AddressResponse response = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        log.info("DELETE /api/v1/users/{}/addresses/{} - Deleting address", userId, addressId);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
