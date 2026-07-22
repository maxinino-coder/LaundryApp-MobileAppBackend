package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class availableOrderInfo {

    private UUID orderId;
    private String       orderNumber;
    private OrderStatus status;

    // Business info — so the rider knows where to pick up from
    private String       businessName;
    private String       pickupAddress;
    private Double        pickupLat;
    private Double        pickupLng;

    // Delivery destination — so the rider can judge distance/effort
    private String       deliveryAddress;
    private Double        deliveryLat;
    private Double        deliveryLng;

    // What the rider earns for taking this job
    private BigDecimal deliveryFee;

    // Timing context
    private OffsetDateTime createdAt;
    private OffsetDateTime pickupTime;
}