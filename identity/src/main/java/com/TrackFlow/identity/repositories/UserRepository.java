package com.TrackFlow.identity.repositories;

import com.TrackFlow.identity.entities.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByCin(String cin);

    boolean existsByEmail(String email);

    boolean existsByCin(@NotBlank @Size(max = 20) String cin);
}
