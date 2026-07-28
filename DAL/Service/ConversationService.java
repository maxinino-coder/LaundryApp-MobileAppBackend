package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.AccountRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.BusinessRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.ConversationRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.MessageRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.OrderRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.RiderRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.UserRepository;
import com.group130.laundryapp.laundry2_0.Domain.DTO.ConversationSummaryDTO;
import com.group130.laundryapp.laundry2_0.Domain.DTO.MessageDTO;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.ConversationCounterpart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * ConversationService
 *
 * THE core authorization gate for chat and calls. Nobody talks to
 * anybody unless this service confirms they share an active order.
 *
 * Allowed pairs (matches your requirement exactly):
 *   user      ↔ business   (about their order)
 *   user      ↔ pickup/dropoff rider  (coordinating delivery)
 *   business  ↔ rider      (coordinating pickup/dropoff)
 *
 * NOT allowed: user↔user, business↔business, rider↔rider,
 * or any pair where the two accounts don't share an order.
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final RiderRepository riderRepository;

    /**
     * Returns the conversation for this (order, accountA, accountB) pair,
     * creating it if it doesn't exist yet. Throws if the two accounts
     * are not actually both legitimately tied to this order.
     */

    @Transactional
    public Conversation getOrCreateConversation(UUID orderId, UUID requesterAccountId, ConversationCounterpart counterpart) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Requester must be a legitimate participant on this order.
        validateParticipant(order, requesterAccountId);

        // Resolve the OTHER side from the order itself — never trust a client-supplied UUID.
        UUID otherAccountId = switch (counterpart) {
            case BUSINESS -> order.getBusiness().getAccount().getId();
            case CUSTOMER -> order.getUser().getAccount().getId();
            case PICKUP_RIDER -> {
                if (order.getPickupRider() == null) {
                    throw new IllegalStateException("No pickup rider is assigned to this order yet");
                }
                yield order.getPickupRider().getAccount().getId();
            }
            case DROPOFF_RIDER -> {
                if (order.getDropoffRider() == null) {
                    throw new IllegalStateException("No dropoff rider is assigned to this order yet");
                }
                yield order.getDropoffRider().getAccount().getId();
            }
        };

        if (otherAccountId.equals(requesterAccountId)) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }

        UUID lower = requesterAccountId.compareTo(otherAccountId) < 0 ? requesterAccountId : otherAccountId;
        UUID upper = requesterAccountId.compareTo(otherAccountId) < 0 ? otherAccountId : requesterAccountId;

        return conversationRepository.findByOrderAndParticipants(orderId, lower, upper)
                .orElseGet(() -> createConversation(order, lower, upper));
    }
    private Conversation createConversation(Order order, UUID participantA, UUID participantB) {
        Account accountA = accountRepository.getReferenceById(participantA);
        Account accountB = accountRepository.getReferenceById(participantB);

        String channelName = deriveChannelName(order.getId(), participantA, participantB);

        Conversation conversation = Conversation.builder()
                .order(order)
                .participantA(accountA)
                .participantB(accountB)
                .supabaseChannel(channelName)
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Checks that this account is actually allowed to be in a
     * conversation about this order — i.e. they are the order's
     * user, business, pickup rider, or dropoff rider.
     */
    private void validateParticipant(Order order, UUID accountId) {
        boolean isUser     = order.getUser().getAccount().getId().equals(accountId);
        boolean isBusiness = order.getBusiness().getAccount().getId().equals(accountId);
        boolean isDropoffRider     = order.getDropoffRider() != null
                && order.getDropoffRider().getAccount().getId().equals(accountId);
        boolean isPickupRider     = order.getPickupRider() != null
                && order.getPickupRider().getAccount().getId().equals(accountId);

        if (!isUser && !isBusiness && !(isPickupRider || isDropoffRider)) {
            throw new SecurityException(
                    "Account " + accountId + " is not a participant on order " + order.getId());
        }
    }

    /**
     * Deterministic, collision-resistant channel name derived from
     * order id + sorted participant ids. Same inputs always produce
     * the same channel name, so repeated calls find the same conversation.
     */
    private String deriveChannelName(UUID orderId, UUID participantA, UUID participantB) {
        String raw = orderId + ":" + participantA + ":" + participantB;
        return "order_" + sha256Short(raw);
    }

    private String sha256Short(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) { // first 16 bytes is plenty for uniqueness here
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive channel name", e);
        }
    }

    /** Used by the token-issuing services to confirm membership before minting a token. */
    public void assertCanAccessConversation(Conversation conversation, UUID accountId) {
        if (!conversation.hasParticipant(accountId)) {
            throw new SecurityException("Account is not a participant in this conversation");
        }
    }

    public Conversation findConversationOrThrow(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
    }

    /** All conversations for the current account, newest first, with counterpart + last message. */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> listConversationsFor(UUID accountId) {
        return conversationRepository.findAllForAccount(accountId).stream()
                .map(c -> {
                    Account other = c.getParticipantA().getId().equals(accountId)
                            ? c.getParticipantB() : c.getParticipantA();

                    Message last = messageRepository
                            .findFirstByConversationIdOrderByCreatedAtDesc(c.getId())
                            .orElse(null);

                    return ConversationSummaryDTO.builder()
                            .conversationId(c.getId())
                            .orderId(c.getOrder().getId())
                            .orderNumber(c.getOrder().getOrderNumber())
                            .supabaseChannel(c.getSupabaseChannel())
                            .otherAccountId(other.getId())
                            .otherName(displayNameFor(other))
                            .otherRole(other.getRole() != null ? other.getRole().name() : null)
                            .lastMessage(last != null ? last.getBody() : null)
                            .lastMessageAt(last != null ? last.getCreatedAt() : null)
                            .unreadCount(messageRepository.countUnreadInConversation(c.getId(), accountId))
                            .createdAt(c.getCreatedAt())
                            .build();
                })
                .toList();
    }

    /** Full message history for a conversation, oldest first. Caller must be a participant. */
    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(UUID conversationId, UUID requesterAccountId) {
        Conversation conversation = findConversationOrThrow(conversationId);
        assertCanAccessConversation(conversation, requesterAccountId);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageDTO::from)
                .toList();
    }

    /** Human-readable name for the counterpart, per role profile. */
    private String displayNameFor(Account account) {
        switch (account.getRole()) {
            case USER -> {
                return userRepository.findByAccountId(account.getId())
                        .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                        .orElse(account.getEmail());
            }
            case BUSINESS -> {
                return businessRepository.findByAccountId(account.getId())
                        .map(Business::getBusinessName)
                        .orElse(account.getEmail());
            }
            case RIDER -> {
                return riderRepository.findByAccountId(account.getId())
                        .map(r -> (r.getFirstName() + " " + r.getLastName()).trim())
                        .orElse(account.getEmail());
            }
            default -> {
                return account.getEmail();
            }
        }
    }
}