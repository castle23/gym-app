package com.gym.auth.repository;

import com.gym.auth.entity.ProfessionalRequest;
import com.gym.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalRequestRepository extends JpaRepository<ProfessionalRequest, Long> {
    List<ProfessionalRequest> findByProfessionalAndStatus(User professional, ProfessionalRequest.RequestStatus status);
    List<ProfessionalRequest> findByRequestingUserAndStatus(User requestingUser, ProfessionalRequest.RequestStatus status);
    Optional<ProfessionalRequest> findByRequestingUserAndProfessional(User requestingUser, User professional);
}
