package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.CallLogRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.Domain.Entity.CallLog;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Conversation;
import com.group130.laundryapp.laundry2_0.Domain.Enum.CallStatus;
import io.agora.media.RtcTokenBuilder2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * AgoraCallService
 *
 * Issues short-lived Agora RTC tokens scoped to one conversation's
 * channel. Voice audio itself is NEVER relayed through this backend
 * — Agora's SDK on each frontend connects directly to Agora's
 * infrastructure using this token. This backend's only job is
 * deciding WHO is allowed to join WHICH channel.
 *
 * Requires the official Agora Java token builder:
 *   <dependency>
 *     <groupId>io.agora</groupId>
 *     <artifactId>authentication</artifactId>
 *     <version>2.1.1</version>
 *   </dependency>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgoraCallService {

    private final ConversationService conversationService;
    private final CallLogRepository   callLogRepository;

    @Value("${agora.app-id}")
    private String appId;

    @Value("${agora.app-certificate}")
    private String appCertificate;

    @Value("${agora.token-expiry-seconds:3600}")
    private int tokenExpirySeconds;

    /**
     * Called when a user/business/rider taps "call" inside a conversation.
     * Validates they're a legitimate participant, logs the call attempt,
     * and returns an Agora token good for tokenExpirySeconds.
     */
    @Transactional
    public CallTokenResponse initiateCall(UUID conversationId, UUID callerId, UUID calleeId) {
        Conversation conversation = conversationService.findConversationOrThrow(conversationId);

        conversationService.assertCanAccessConversation(conversation, callerId);
        conversationService.assertCanAccessConversation(conversation, calleeId);

        // Agora channel name reuses the conversation's Supabase channel
        // name for simplicity — one consistent identifier per conversation
        // across both chat and call systems.
        String agoraChannel = conversation.getSupabaseChannel();

        // Numeric UID required by Agora — derive deterministically from
        // the account's UUID so the same account always gets the same
        // Agora UID within a channel (helps with reconnect/dedup logic).
        int agoraUid = Math.abs(callerId.hashCode());

        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();
        String token = tokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                agoraChannel,
                agoraUid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                tokenExpirySeconds,
                tokenExpirySeconds
        );

        CallLog callLog = CallLog.builder()
                .conversation(conversation)
                .caller(Account.builder().id(callerId).build())
                .callee(Account.builder().id(calleeId).build())
                .agoraChannel(agoraChannel)
                .status(CallStatus.INITIATED)
                .startedAt(OffsetDateTime.now())
                .build();
        callLogRepository.save(callLog);

        log.info("Call initiated: conversation {} caller {} callee {}",
                conversationId, callerId, calleeId);

        return new CallTokenResponse(token, agoraChannel, agoraUid, callLog.getId());
    }

    @Transactional
    public void endCall(UUID callLogId, int durationSeconds) {
        CallLog callLog = callLogRepository.findById(callLogId)
                .orElseThrow(() -> new IllegalArgumentException("Call log not found"));

        callLog.setStatus(CallStatus.ENDED);
        callLog.setEndedAt(OffsetDateTime.now());
        callLog.setDurationSeconds(durationSeconds);
        callLogRepository.save(callLog);
    }

    public record CallTokenResponse(
            String token,
            String channelName,
            int agoraUid,
            UUID callLogId
    ) {}
}
