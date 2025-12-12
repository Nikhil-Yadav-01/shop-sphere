package com.rudraksha.shopsphere.user.service.impl;

import com.rudraksha.shopsphere.user.dto.request.CreateAddressRequest;
import com.rudraksha.shopsphere.user.dto.response.AddressResponse;
import com.rudraksha.shopsphere.user.entity.Address;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import com.rudraksha.shopsphere.user.mapper.UserMapper;
import com.rudraksha.shopsphere.user.repository.AddressRepository;
import com.rudraksha.shopsphere.user.repository.UserProfileRepository;
import com.rudraksha.shopsphere.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        log.debug("Fetching all addresses for userId: {}", userId);
        
        if (!userProfileRepository.existsById(userId)) {
            throw new RuntimeException("User profile not found");
        }

        List<Address> addresses = addressRepository.findByUserProfileId(userId);
        return addresses.stream()
                .map(userMapper::toAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(UUID userId, UUID addressId) {
        log.debug("Fetching address {} for userId: {}", addressId, userId);
        
        if (!userProfileRepository.existsById(userId)) {
            throw new RuntimeException("User profile not found");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUserProfile().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to this user");
        }

        return userMapper.toAddressResponse(address);
    }

    @Override
    public AddressResponse createAddress(UUID userId, CreateAddressRequest request) {
        log.debug("Creating address for userId: {}", userId);

        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        Address address = userMapper.toAddressEntity(request);
        address.setUserProfile(userProfile);
        userProfile.addAddress(address);

        Address savedAddress = addressRepository.save(address);
        log.info("Address created with id: {} for userId: {}", savedAddress.getId(), userId);
        return userMapper.toAddressResponse(savedAddress);
    }

    @Override
    public AddressResponse updateAddress(UUID userId, UUID addressId, CreateAddressRequest request) {
        log.debug("Updating address {} for userId: {}", addressId, userId);

        if (!userProfileRepository.existsById(userId)) {
            throw new RuntimeException("User profile not found");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUserProfile().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to this user");
        }

        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setIsDefault(request.getIsDefault());

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated with id: {}", updatedAddress.getId());
        return userMapper.toAddressResponse(updatedAddress);
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        log.debug("Deleting address {} for userId: {}", addressId, userId);

        if (!userProfileRepository.existsById(userId)) {
            throw new RuntimeException("User profile not found");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUserProfile().getId().equals(userId)) {
            throw new RuntimeException("Address does not belong to this user");
        }

        addressRepository.deleteById(addressId);
        log.info("Address deleted with id: {}", addressId);
    }
}
