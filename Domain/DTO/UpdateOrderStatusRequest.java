package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    // App sends {"status": "..."} — accept both key spellings
    @JsonAlias("status")
    private OrderStatus orderStatus;
}
