package com.group130.laundryapp.Domain.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_reviews_order_id"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_reviewer_account_id"))
    private Account reviewerAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reviews_business_id"))
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id",
            foreignKey = @ForeignKey(name = "fk_reviews_rider_id"))
    private Rider rider;

    @Column(name = "business_rating", nullable = false)
    private Short businessRating;  // 1–5

    @Column(name = "rider_rating")
    private Short riderRating;     // 1–5, nullable if no rider assigned

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_visible", nullable = false) @Builder.Default
    private boolean isVisible = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
