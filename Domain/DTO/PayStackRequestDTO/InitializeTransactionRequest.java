package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackRequestDTO;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitializeTransactionRequest {
    private String email;
    private Long   amount;          // in subunit — e.g. GHS 50.00 = 5000
    private String reference;       // your own unique transaction reference
    private String subaccount;      // the business's subaccount_code — triggers the split
    private String currency;        // "GHS"
    @JsonProperty("callback_url")
    private String callbackUrl;
    private Map<String, Object> metadata;  // store orderId here for webhook lookup
}
