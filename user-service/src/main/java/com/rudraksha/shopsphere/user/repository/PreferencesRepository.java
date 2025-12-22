package com.rudraksha.shopsphere.user.repository;

import com.rudraksha.shopsphere.user.entity.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreferencesRepository extends JpaRepository<Preferences, UUID> {
    Optional<Preferences> findByUserProfileId(UUID userProfileId);
}
