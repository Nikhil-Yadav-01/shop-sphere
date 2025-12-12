package com.rudraksha.shopsphere.user.mapper;

import com.rudraksha.shopsphere.user.dto.request.CreateAddressRequest;
import com.rudraksha.shopsphere.user.dto.response.AddressResponse;
import com.rudraksha.shopsphere.user.dto.response.UserResponse;
import com.rudraksha.shopsphere.user.entity.Address;
import com.rudraksha.shopsphere.user.entity.UserProfile;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-12T18:03:12+0000",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Ubuntu)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toUserResponse(UserProfile userProfile) {
        if ( userProfile == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.id( userProfile.getId() );
        userResponse.authUserId( userProfile.getAuthUserId() );
        userResponse.phone( userProfile.getPhone() );
        userResponse.dateOfBirth( userProfile.getDateOfBirth() );
        userResponse.avatarUrl( userProfile.getAvatarUrl() );
        userResponse.addresses( addressListToAddressResponseList( userProfile.getAddresses() ) );
        userResponse.createdAt( userProfile.getCreatedAt() );
        userResponse.updatedAt( userProfile.getUpdatedAt() );

        return userResponse.build();
    }

    @Override
    public AddressResponse toAddressResponse(Address address) {
        if ( address == null ) {
            return null;
        }

        AddressResponse.AddressResponseBuilder addressResponse = AddressResponse.builder();

        addressResponse.id( address.getId() );
        addressResponse.addressLine1( address.getAddressLine1() );
        addressResponse.addressLine2( address.getAddressLine2() );
        addressResponse.city( address.getCity() );
        addressResponse.state( address.getState() );
        addressResponse.postalCode( address.getPostalCode() );
        addressResponse.country( address.getCountry() );
        addressResponse.isDefault( address.getIsDefault() );
        addressResponse.createdAt( address.getCreatedAt() );
        addressResponse.updatedAt( address.getUpdatedAt() );

        return addressResponse.build();
    }

    @Override
    public Address toAddressEntity(CreateAddressRequest request) {
        if ( request == null ) {
            return null;
        }

        Address.AddressBuilder address = Address.builder();

        address.addressLine1( request.getAddressLine1() );
        address.addressLine2( request.getAddressLine2() );
        address.city( request.getCity() );
        address.state( request.getState() );
        address.postalCode( request.getPostalCode() );
        address.country( request.getCountry() );
        address.isDefault( request.getIsDefault() );

        return address.build();
    }

    protected List<AddressResponse> addressListToAddressResponseList(List<Address> list) {
        if ( list == null ) {
            return null;
        }

        List<AddressResponse> list1 = new ArrayList<AddressResponse>( list.size() );
        for ( Address address : list ) {
            list1.add( toAddressResponse( address ) );
        }

        return list1;
    }
}
