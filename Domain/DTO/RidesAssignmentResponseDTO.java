package com.group130.laundryapp.laundry2_0.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RidesAssignmentResponseDTO{
    private UUID id;
    private UUID orderId;
    private UUID riderId;
    private UUID businessId;
    private String status;
    private String createdAt;
}
