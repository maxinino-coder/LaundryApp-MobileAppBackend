package com.group130.laundryapp.Domain.Entity;

import com.group130.laundryapp.Domain.Enum.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_payouts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_business_payouts_business_id"))
    private Business business;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_business_payouts_order_id"))
    private Order order;

    @Column(name = "order_revenue",       nullable = false, precision = 12, scale = 2)
    private BigDecimal orderRevenue;

    @Column(name = "platform_commission", nullable = false, precision = 12, scale = 2) @Builder.Default
    private BigDecimal platformCommission = BigDecimal.ZERO;

    @Column(name = "rider_fee",           nullable = false, precision = 12, scale = 2) @Builder.Default
    private BigDecimal riderFee           = BigDecimal.ZERO;

    @Column(name = "net_payout",          nullable = false, precision = 12, scale = 2)
    private BigDecimal netPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15) @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Convenience: compute net from revenue minus commissions. */
    public void computeNetPayout() {
        this.netPayout = orderRevenue.subtract(platformCommission).subtract(riderFee);
    }
}
