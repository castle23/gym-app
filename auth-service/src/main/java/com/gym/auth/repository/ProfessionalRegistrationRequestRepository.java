package com.gym.auth.repository;

import com.gym.auth.entity.ProfessionalRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalRegistrationRequestRepository extends JpaRepository<ProfessionalRegistrationRequest, Long> {
    List<ProfessionalRegistrationRequest> findByStatus(ProfessionalRegistrationRequest.RequestStatus status);
}
