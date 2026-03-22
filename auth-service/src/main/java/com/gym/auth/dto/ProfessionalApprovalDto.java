package com.gym.auth.dto;

import com.gym.auth.entity.ProfessionalRegistrationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProfessionalApprovalDto", description = "Decision for professional registration")
public class ProfessionalApprovalDto {
    @NotNull
    @Schema(description = "Decision status (APPROVED, REJECTED)", example = "APPROVED")
    private ProfessionalRegistrationRequest.RequestStatus status;

    @Schema(description = "Reason for rejection (mandatory if status is REJECTED)", example = "Invalid license number")
    private String rejectionReason;
}
