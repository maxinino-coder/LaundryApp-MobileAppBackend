package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Entity.BusinessPayout;
import com.group130.laundryapp.laundry2_0.Domain.Entity.RiderEarning;
import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiderPayoutDTO {
    private UUID riderID;
    private String riderFirstName;
    private String riderLastName;
    private String riderEmail;
    private String riderPhone;

    // Summary statistics
    private BigDecimal totalEarnings;
    private BigDecimal totalPending;
    private BigDecimal totalSettled;
    private BigDecimal totalFailed;
    private Long pendingCount;
    private Long settledCount;
    private Long failedCount;
    private Long totalOrders;

    // Breakdown by leg type
    private BigDecimal pickupEarnings;
    private BigDecimal dropoffEarnings;

    // Detailed list
    private List<RiderEarning> earnings;

}
