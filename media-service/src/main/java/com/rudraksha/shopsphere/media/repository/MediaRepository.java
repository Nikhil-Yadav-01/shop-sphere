package com.rudraksha.shopsphere.media.repository;

import com.rudraksha.shopsphere.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<Media> findByEntityType(String entityType);
    Optional<Media> findByFileNameAndEntityTypeAndEntityId(String fileName, String entityType, Long entityId);
    List<Media> findByIsActiveTrue();
}
