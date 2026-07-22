package com.group130.laundryapp.laundry2_0.Domain.DTO;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel;
import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateServiceItemRequest {
    private UUID accountId;

    private String name;

    private String description;

    private ServiceCategory category;

    private PricingModel pricingModel;

    private BigDecimal unitPrice;

    private String unit;

    private String imageUrl;
}
