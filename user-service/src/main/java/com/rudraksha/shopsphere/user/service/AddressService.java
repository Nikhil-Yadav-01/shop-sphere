package com.rudraksha.shopsphere.user.service;

import com.rudraksha.shopsphere.user.dto.request.CreateAddressRequest;
import com.rudraksha.shopsphere.user.dto.response.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getUserAddresses(UUID userId);

    AddressResponse getAddress(UUID userId, UUID addressId);

    AddressResponse createAddress(UUID userId, CreateAddressRequest request);

    AddressResponse updateAddress(UUID userId, UUID addressId, CreateAddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);
}
