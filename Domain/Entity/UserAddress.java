package com.group130.laundryapp.laundry2_0.Domain.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_addresses_user_id"))
    private User user;

    @Column(nullable = false, length = 100)
    private String label;   // e.g. Home, Office

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 100) private String city;
    private Double latitude;
    private Double longitude;

    @Column(name = "is_default", nullable = false) @Builder.Default
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
