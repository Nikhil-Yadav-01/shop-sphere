package com.rudraksha.shopsphere.shared.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface BaseRepository<T, ID extends UUID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    Optional<T> findByIdAndDeletedFalse(UUID id);
}
