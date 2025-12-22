package com.rudraksha.shopsphere.user.mapper;

import com.rudraksha.shopsphere.user.dto.request.CreateAddressRequest;
import com.rudraksha.shopsphere.user.dto.response.AddressResponse;
import com.rudraksha.shopsphere.user.dto.response.AddressResponseList;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;
import com.rudraksha.shopsphere.user.entity.Address;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(UserProfile userProfile);

    AddressResponse toAddressResponse(Address address);

    AddressResponseList toAddressResponseList(Address address);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Address toAddressEntity(CreateAddressRequest request);
}
