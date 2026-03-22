package com.gym.auth.repository;

import com.gym.auth.entity.ProfessionalRegistrationRequest;
import com.gym.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalRegistrationRequestRepository extends JpaRepository<ProfessionalRegistrationRequest, Long> {
    boolean existsByUser(User user);
    List<ProfessionalRegistrationRequest> findByStatus(ProfessionalRegistrationRequest.RequestStatus status);
}
