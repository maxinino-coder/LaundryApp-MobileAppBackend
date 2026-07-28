package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Enum.ConversationCounterpart;

import java.util.UUID;

public record StartConversationRequest(UUID orderId, ConversationCounterpart counterpart) {}

