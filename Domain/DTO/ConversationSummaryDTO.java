package com.group130.laundryapp.laundry2_0.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One row in a user's conversation list. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationSummaryDTO {
    private UUID conversationId;
    private UUID orderId;
    private String orderNumber;
    private String supabaseChannel;
    private UUID otherAccountId;
    private String otherName;
    private String otherRole;      // USER | BUSINESS | RIDER
    private String lastMessage;
    private OffsetDateTime lastMessageAt;
    private long unreadCount;
    private OffsetDateTime createdAt;
}
