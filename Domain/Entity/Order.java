package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id",          columnList = "user_id"),
                @Index(name = "idx_orders_business_id",      columnList = "business_id"),
                @Index(name = "idx_orders_pickup_rider_id",  columnList = "pickup_rider_id"),
                @Index(name = "idx_orders_dropoff_rider_id", columnList = "dropoff_rider_id"),
                @Index(name = "idx_orders_status",           columnList = "status"),
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, length = 20, updatable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user_id"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_business_id"))
    private Business business;

    // ── Two independent rider slots ──────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_rider_id",
            foreignKey = @ForeignKey(name = "fk_orders_pickup_rider_id"))
    private Rider pickupRider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropoff_rider_id",
            foreignKey = @ForeignKey(name = "fk_orders_dropoff_rider_id"))
    private Rider dropoffRider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "delivery_requested", nullable = false)
    @Builder.Default
    private boolean deliveryRequested = false;

    // Pickup
    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;
    @Column(name = "pickup_lat")  private Double pickupLat;
    @Column(name = "pickup_lng")  private Double pickupLng;
    @Column(name = "pickup_time") private OffsetDateTime pickupTime;
    @Column(name = "pickup_confirmed_at") private OffsetDateTime pickupConfirmedAt;

    // Delivery / Dropoff
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;
    @Column(name = "delivery_lat")  private Double deliveryLat;
    @Column(name = "delivery_lng")  private Double deliveryLng;
    @Column(name = "delivery_time") private OffsetDateTime deliveryTime;
    @Column(name = "dropoff_confirmed_at") private OffsetDateTime dropoffConfirmedAt;

    // Financials
    @Column(nullable = false, precision = 12, scale = 2) @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "pickup_fee", nullable = false, precision = 10, scale = 2) @Builder.Default
    private BigDecimal pickupFee = BigDecimal.ZERO;

    @Column(name = "dropoff_fee", nullable = false, precision = 10, scale = 2) @Builder.Default
    private BigDecimal dropoffFee = BigDecimal.ZERO;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2) @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO; // legacy/combined total, kept for reporting

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")  private String notes;
    @Column(name = "cancelled_reason", columnDefinition = "TEXT") private String cancelledReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    // Helper
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }



}