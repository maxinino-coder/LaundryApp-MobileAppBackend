//package com.group130.laundryapp.laundry2_0.Domain.Entity;
//
//import com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel;
//import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.math.BigDecimal;
//import java.time.OffsetDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "service_items",
//        uniqueConstraints = @UniqueConstraint(name = "uq_service_items_business_name",
//                columnNames = {"business_id", "name"}))
//@Getter @Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ServiceItem {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "business_id", nullable = false,
//            foreignKey = @ForeignKey(name = "fk_service_items_business_id"))
//    private Business business;
//
//    @Column(nullable = false, length = 255)
//    private String name;
//
//    @Column(columnDefinition = "TEXT")
//    private String description;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 30)
//    private ServiceCategory category;
//
//    @Column(name = "pricing_model", nullable = false, precision = 10, scale = 2)
//    private PricingModel pricing_model;
//    @Column(
//            name = "unit_price",
//            nullable = false,
//            precision = 10,
//            scale = 2
//    )
//    private BigDecimal unitPrice;
//    @Column(nullable = false, length = 50)
//    @Builder.Default
//    private String unit = "piece";
//
//    @Column(name = "image_url", columnDefinition = "TEXT")
//    private String imageUrl;
//
//    @Column(name = "is_active", nullable = false) @Builder.Default
//    private boolean isActive = true;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private OffsetDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private OffsetDateTime updatedAt;
//
//
//}
//
