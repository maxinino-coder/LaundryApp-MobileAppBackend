package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Flat message view — avoids serializing the lazy conversation/sender JPA graph. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String body;
    private boolean isRead;
    private OffsetDateTime createdAt;

    public static MessageDTO from(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .body(m.getBody())
                .isRead(m.isRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
