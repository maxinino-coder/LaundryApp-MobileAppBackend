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
class CreateTransferRecipientRequest {
    private String type;             // "mobile_money"
    private String name;
    @JsonProperty("account_number")
    private String accountNumber;    // MoMo number
    @JsonProperty("bank_code")
    private String bankCode;         // MTN = "MTN", Vodafone = "VOD", AirtelTigo = "ATL"
    private String currency;         // "GHS"
}
