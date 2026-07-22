package com.group130.laundryapp.laundry2_0.Domain.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversations_order_participants",
                columnNames = {"order_id", "participant_a_id", "participant_b_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversations_order_id"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_a_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversations_participant_a"))
    private Account participantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_b_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversations_participant_b"))
    private Account participantB;

    /** Deterministic Supabase channel name — see ConversationService for derivation. */
    @Column(name = "supabase_channel", nullable = false, unique = true, length = 100)
    private String supabaseChannel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public boolean hasParticipant(UUID accountId) {
        return participantA.getId().equals(accountId) || participantB.getId().equals(accountId);
    }
}