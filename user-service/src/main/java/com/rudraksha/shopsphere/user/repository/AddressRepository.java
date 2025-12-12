package com.rudraksha.shopsphere.user.repository;

import com.rudraksha.shopsphere.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserProfileId(UUID userProfileId);

    Address findDefaultByUserProfileId(UUID userProfileId);
}
