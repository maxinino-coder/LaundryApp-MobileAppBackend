package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackRequestDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class InitiateTransferRequest {
    private String source;          // always "balance"
    private Long   amount;          // in subunit
    private String recipient;       // the rider's recipient_code
    private String reason;
    private String reference;       // your own unique transfer reference
}
