package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.AuthSupport.UserContextHolder;
import com.group130.laundryapp.laundry2_0.DAL.Service.AgoraCallService;
import com.group130.laundryapp.laundry2_0.DAL.Service.AgoraCallService.CallTokenResponse;
import com.group130.laundryapp.laundry2_0.Domain.DTO.EndCallRequest;
import com.group130.laundryapp.laundry2_0.Domain.DTO.StartCallRequest;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * POST /api/v1/conversations/{conversationId}/call/start
 * Body: { "calleeId": "..." }
 *
 * Returns an Agora token + channel name. Frontend passes these
 * directly to the Agora SDK to join the voice channel — this
 * backend is out of the audio path entirely after this call.
 */
@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/call")
@RequiredArgsConstructor
public class CallController {

    private final AgoraCallService  agoraCallService;
    private final UserContextHolder userContextHolder;

    @PostMapping("/start")
    public ResponseEntity<CallTokenResponse> startCall(
            @PathVariable UUID conversationId,
            @RequestBody StartCallRequest req
    ) {
        Account caller = userContextHolder.getCurrentAccount();
        CallTokenResponse response = agoraCallService.initiateCall(
                conversationId, caller.getId(), req.getCalleeId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{callLogId}/end")
    public ResponseEntity<Void> endCall(
            @PathVariable UUID conversationId,
            @PathVariable UUID callLogId,
            @RequestBody EndCallRequest req
    ) {
        agoraCallService.endCall(callLogId, req.getDurationSeconds());
        return ResponseEntity.noContent().build();
    }

}
