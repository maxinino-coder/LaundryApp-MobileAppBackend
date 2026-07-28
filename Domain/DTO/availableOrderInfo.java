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
    private String       businessAddress;
    private Double        businessLat;
    private Double        businessLng;

    private String       pickupAddress;
    private Double        pickupLat;
    private Double        pickupLng;

    private String       deliveryAddress;
    private Double        deliveryLat;
    private Double        deliveryLng;

    private BigDecimal deliveryFee;

    private OffsetDateTime createdAt;
    private OffsetDateTime pickupTime;

    /** "PICKUP" (customer → business) or "DELIVERY" (business → customer). */
    private String jobType;
}