package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackRequestDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class CreateSubaccountRequest {
    @JsonProperty("business_name")
    private String businessName;
    @JsonProperty("settlement_bank")
    private String settlementBank;   // bank code, e.g. "044"
    @JsonProperty("account_number")
    private String accountNumber;
    @JsonProperty("percentage_charge")
    private Double percentageCharge; // e.g. 80.0 means business keeps 80%, platform gets 20%
}