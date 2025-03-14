package com.expen.auth_service.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.expen.auth_service.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySlug(String slug);
}
