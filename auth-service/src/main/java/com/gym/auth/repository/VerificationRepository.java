package com.gym.auth.repository;

import com.gym.auth.entity.User;
import com.gym.auth.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByUserAndCode(User user, String code);
    Optional<Verification> findByUserAndTypeAndVerified(User user, Verification.VerificationType type, Boolean verified);
}
