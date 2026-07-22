package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.group130.laundryapp.laundry2_0.Domain.Enum.CallStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_logs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_call_logs_conversation_id"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_call_logs_caller_id"))
    private Account caller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "callee_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_call_logs_callee_id"))
    private Account callee;

    @Column(name = "agora_channel", nullable = false, length = 100)
    private String agoraChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15) @Builder.Default
    private CallStatus status = CallStatus.INITIATED;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

}
