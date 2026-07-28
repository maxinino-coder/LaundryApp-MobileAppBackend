package com.group130.laundryapp.laundry2_0.Domain.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_businesses_account_id"))
    private Account account;

    @Column(name = "business_name", nullable = false, length = 255)
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url",   columnDefinition = "TEXT") private String logoUrl;
    @Column(name = "banner_url", columnDefinition = "TEXT") private String bannerUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 100) private String city;
    private Double latitude;
    private Double longitude;

//    @Column(name = "is_approved", nullable = false) @Builder.Default
//    private boolean isApproved = false;

//    @Column(name = "is_open", nullable = false) @Builder.Default
//    private boolean isOpen = false;

    @Column(name = "opening_time") private LocalTime openingTime;
    @Column(name = "closing_time") private LocalTime closingTime;

    @Column(name = "bank_name",        length = 100) private String bankName;
    @Column(name = "bank_account_no",  length = 20)  private String bankAccountNo;
    @Column(name = "bank_account_name",length = 150) private String bankAccountName;
    @Column(name = "momo_number",      length = 20)  private String momoNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false)
    @Builder.Default
    private com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel pricingModel = com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel.PER_ITEM;

    @Column(name = "price_per_kg", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal pricePerKg = new java.math.BigDecimal("15.00");

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default private List<Rider>       riders       = new ArrayList<>();

//    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default private List<ServiceItem> serviceItems = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "business", fetch = FetchType.LAZY)
    @Builder.Default private List<Order>       orders       = new ArrayList<>();

    @Column(name = "paystack_subaccount_code", length = 50)
    private String paystackSubaccountCode;

    @Column(name = "paystack_subaccount_id", length = 50)
    private String paystackSubaccountId;    // ← Optional: store the ID too
}
