package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackResponseDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class TransferRecipientData {
    @JsonProperty("recipient_code")
    private String recipientCode;
}
