package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemsRequest {
    // The catalog is category-based (ServiceItem entity was intentionally removed)
    private ServiceCategory serviceCategory;
    private int quantity;
    private BigDecimal unitPrice;
    private String note;
}
