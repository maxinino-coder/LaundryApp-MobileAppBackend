package com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackResponseDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class WebhookPayload {
    private String event;
    private Map<String, Object> data;
}