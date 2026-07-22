package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.AuthSupport.UserContextHolder;
import com.group130.laundryapp.laundry2_0.DAL.Service.MessageService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.MessageDTO;
import com.group130.laundryapp.laundry2_0.Domain.DTO.SendMessageRequest;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * POST /api/v1/conversations/{conversationId}/messages
 *
 * Sends a message. The frontend does NOT write directly to
 * Supabase — every message goes through here first for
 * validation, then gets mirrored to Supabase for realtime push.
 *
 * Frontend realtime subscription (client-side, NOT this backend):
 *   supabase
 *     .channel('messages:' + conversationId)
 *     .on('postgres_changes',
 *         { event: 'INSERT', schema: 'public', table: 'messages',
 *           filter: `conversation_id=eq.${conversationId}` },
 *         (payload) => appendMessageToUI(payload.new))
 *     .subscribe()
 */
@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService    messageService;
    private final UserContextHolder userContextHolder;

    @PostMapping
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest req
    ) {
        Account sender = userContextHolder.getCurrentAccount();
        Message message = messageService.sendMessage(conversationId, sender.getId(), req.getBody());
        return ResponseEntity.ok(MessageDTO.from(message));
    }


}
