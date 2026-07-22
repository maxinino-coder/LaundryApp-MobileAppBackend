package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Entity.BusinessPayout;
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
public class BusinessPayoutDTO {
    private UUID businessId;
    private String businessName;
    private BigDecimal totalRevenue;
    private BigDecimal totalCommission;
    private BigDecimal totalRiderFee;
    private BigDecimal totalNetPayout;
    private Long pendingCount;
    private Long settledCount;
    private List<BusinessPayout> payouts;
}