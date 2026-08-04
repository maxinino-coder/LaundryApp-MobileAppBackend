package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.Payment;
import com.group130.laundryapp.Domain.Enum.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByTransactionRef(String transactionRef);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.payerAccount.id = :accountId ORDER BY p.createdAt DESC")
    Page<Payment> findByPayerAccountId(@Param("accountId") UUID accountId, Pageable pageable);
}

