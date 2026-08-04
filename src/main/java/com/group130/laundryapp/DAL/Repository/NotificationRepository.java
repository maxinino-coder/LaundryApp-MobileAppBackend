package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.account.id = :accountId AND n.isRead = false")
    long countUnreadByAccountId(@Param("accountId") UUID accountId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.account.id = :accountId")
    void markAllReadByAccountId(@Param("accountId") UUID accountId);
}

