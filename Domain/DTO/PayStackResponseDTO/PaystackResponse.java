package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackResponseDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaystackResponse<T> {
    private boolean status;
    private String  message;
    private T       data;
}
