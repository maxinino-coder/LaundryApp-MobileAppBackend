package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.AuthSupport.UserContextHolder;
import com.group130.laundryapp.laundry2_0.DAL.Service.ConversationService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.ConversationSummaryDTO;
import com.group130.laundryapp.laundry2_0.Domain.DTO.MessageDTO;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Conversation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POST /api/v1/conversations/start          — get or create a conversation for an order
 * GET  /api/v1/conversations                — list my conversations (newest first)
 * GET  /api/v1/conversations/{id}/messages  — full history, oldest first
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserContextHolder userContextHolder;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startConversation(@RequestBody StartConversationRequest req) {
        Account requester = userContextHolder.getCurrentAccount();

        Conversation conversation = conversationService.getOrCreateConversation(
                req.orderId(), requester.getId(), req.otherAccountId());

        return ResponseEntity.ok(Map.of(
                "conversation_id", conversation.getId().toString(),
                "supabase_channel", conversation.getSupabaseChannel()
        ));
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryDTO>> listConversations() {
        Account requester = userContextHolder.getCurrentAccount();
        return ResponseEntity.ok(conversationService.listConversationsFor(requester.getId()));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable UUID conversationId) {
        Account requester = userContextHolder.getCurrentAccount();
        return ResponseEntity.ok(conversationService.getMessages(conversationId, requester.getId()));
    }

    public record StartConversationRequest(UUID orderId, UUID otherAccountId) {}
}
