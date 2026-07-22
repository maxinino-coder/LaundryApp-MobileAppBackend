package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.google.auto.value.AutoValue;
import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private  ServiceCategory serviceCategory;
//    private Double ServiceItemId;
    private Integer Quantity;
//    private Float   Weight_kg;
    private BigDecimal UnitPrice;
    private BigDecimal  LineTotal;
    private String Notes;
    private OffsetDateTime CreatedAt;
}
