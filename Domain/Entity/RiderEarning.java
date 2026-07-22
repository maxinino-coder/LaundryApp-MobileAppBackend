package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rider_earnings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rider_earnings_order_leg",
                columnNames = {"order_id", "leg"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiderEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rider_earnings_rider_id"))
    private Rider rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rider_earnings_order_id"))
    private Order order;

    /** Which leg this earning is for — a pickup rider and dropoff
     *  rider on the same order each get their own earning row. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentType leg;   // RIDE_PICKUP or RIDE_DROPOFF only

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15) @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "paystack_transfer_code", length = 50)
    private String paystackTransferCode;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum SettlementStatus { PENDING, SETTLED, FAILED }
}