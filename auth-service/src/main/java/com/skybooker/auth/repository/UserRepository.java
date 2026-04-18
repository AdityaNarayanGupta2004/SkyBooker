package com.skybooker.auth.repository;

import com.skybooker.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // Admin already exist karta hai check karne ke liye
    boolean existsByRole(String role);

//    to count the admin registration number
    long countByRole(String role);
}