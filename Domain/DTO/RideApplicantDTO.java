package com.group130.laundryapp.laundry2_0.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Flattened view of a RidesAssignment for the user's "choose your rider"
 * screen — avoids serializing the lazy Order/Rider JPA graph.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RideApplicantDTO {
    private UUID applicationId;
    private UUID orderId;
    private UUID riderId;
    private String riderFirstName;
    private String riderLastName;
    private String vehicleType;
    private String vehiclePlate;
    private String status;
    private OffsetDateTime appliedAt;
}
