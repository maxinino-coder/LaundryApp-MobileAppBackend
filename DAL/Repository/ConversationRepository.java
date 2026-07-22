package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findBySupabaseChannel(String channel);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.order.id = :orderId
          AND ((c.participantA.id = :accountA AND c.participantB.id = :accountB)
            OR (c.participantA.id = :accountB AND c.participantB.id = :accountA))
        """)
    Optional<Conversation> findByOrderAndParticipants(
            @Param("orderId") UUID orderId,
            @Param("accountA") UUID accountA,
            @Param("accountB") UUID accountB);

    @Query("""
        SELECT c FROM Conversation c
        WHERE c.participantA.id = :accountId OR c.participantB.id = :accountId
        ORDER BY c.createdAt DESC
        """)
    List<Conversation> findAllForAccount(@Param("accountId") UUID accountId);
}
