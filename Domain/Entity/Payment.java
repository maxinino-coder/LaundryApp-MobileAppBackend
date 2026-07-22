package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentMethod;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_payments_order_type",
                columnNames = {"order_id", "payment_type"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_order_id"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_payer_account_id"))
    private Account payerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 15)
    private PaymentType paymentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3) @Builder.Default
    private String currency = "GHS";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15) @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 50)
    private String provider;

    @Column(name = "transaction_ref", unique = true, length = 255)
    private String transactionRef;

    @Column(name = "paystack_subaccount_code", length = 50)
    private String paystackSubaccountCode;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}