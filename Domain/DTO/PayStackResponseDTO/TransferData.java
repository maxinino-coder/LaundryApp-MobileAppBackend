package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackResponseDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class TransferData {
    private String reference;
    private String status;   // "success", "pending", "failed"
}
