package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.MessageRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Conversation;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

/**
 * MessageService
 *
 * Every message write goes through here — never directly from the
 * frontend to Supabase. This is what makes the "simpler alternative"
 * pattern secure without needing RLS+JWT complexity:
 *
 *   1. ConversationService confirms sender is a legitimate participant
 *   2. Message is saved to YOUR Postgres (messages table) — permanent record
 *   3. Message is ALSO inserted into Supabase's messages table via
 *      the service-role WebClient — this insert is what triggers
 *      Supabase Realtime to push the new row to subscribed frontends
 *      instantly, with no polling and no backend relay needed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository    messageRepository;
    private final ConversationService  conversationService;
    private final WebClient            supabaseWebClient;

    @Transactional
    public Message sendMessage(UUID conversationId, UUID senderAccountId, String body) {

        Conversation conversation = conversationService.findConversationOrThrow(conversationId);
        conversationService.assertCanAccessConversation(conversation, senderAccountId);

        // 1. Save permanently in your own database
        Message message = Message.builder()
                .conversation(conversation)
                .sender(Account.builder().id(senderAccountId).build()) // reference only
                .body(body)
                .build();
        Message saved = messageRepository.save(message);

        // 2. Mirror into Supabase — this insert is what triggers realtime
        //    delivery to any frontend subscribed to this conversation's channel
        mirrorToSupabase(saved, conversation);

        return saved;
    }

    private void mirrorToSupabase(Message message, Conversation conversation) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", message.getId().toString(),
                    "conversation_id", conversation.getId().toString(),
                    "sender_id", message.getSender().getId().toString(),
                    "body", message.getBody(),
                    "created_at", message.getCreatedAt().toString()
            );

            supabaseWebClient.post()
                    .uri("/messages")
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (Exception e) {
            // Don't fail the whole send if Supabase mirror fails —
            // the message is still safely persisted in your own DB.
            // The recipient just won't get instant realtime delivery;
            // they'll see it on next poll/refresh instead.
            log.error("Failed to mirror message {} to Supabase — realtime delivery degraded",
                    message.getId(), e);
        }
    }
}
