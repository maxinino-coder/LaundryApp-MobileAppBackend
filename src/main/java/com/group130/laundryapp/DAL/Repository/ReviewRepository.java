package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByOrderId(UUID orderId);

    Page<Review> findByBusinessIdAndIsVisibleTrue(UUID businessId, Pageable pageable);
    Page<Review> findByRiderIdAndIsVisibleTrue(UUID riderId, Pageable pageable);

    @Query("SELECT AVG(r.businessRating) FROM Review r WHERE r.business.id = :businessId AND r.isVisible = true")
    Optional<Double> averageBusinessRating(@Param("businessId") UUID businessId);

    @Query("SELECT AVG(r.riderRating) FROM Review r WHERE r.rider.id = :riderId AND r.riderRating IS NOT NULL AND r.isVisible = true")
    Optional<Double> averageRiderRating(@Param("riderId") UUID riderId);
}
