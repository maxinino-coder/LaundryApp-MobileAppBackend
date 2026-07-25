package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.group130.laundryapp.laundry2_0.Domain.Enum.RiderType;
import com.group130.laundryapp.laundry2_0.Domain.Enum.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "riders")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_riders_account_id"))
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id",
            foreignKey = @ForeignKey(name = "fk_riders_business_id"))
    private Business business;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    // ✅ Fix 1: was already correct
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    // ✅ Fix 2: added @Enumerated(EnumType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "rider_type", nullable = false, length = 20)
    private RiderType riderType;

    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = false;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private boolean isApproved = false;

    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "last_location_at")
    private OffsetDateTime lastLocationAt;

    @Column(name = "momo_number", length = 20)
    private String momoNumber;

    @Column(name = "bank_account_no", length = 20)
    private String bankAccountNo;

    @Column(name = "paystack_recipient_code", length = 50)
    private String paystackRecipientCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ✅ Fix 3: removed pickupRider and dropoffRider — those are Order columns, not Rider columns

    @OneToMany(mappedBy = "rider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RiderEarning> earnings = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }
}