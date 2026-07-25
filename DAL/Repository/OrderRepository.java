package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.User;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Order;   // ← correct
// import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

//    Optional<Order> findByOrderNumber(String orderNumber);
//
//    Page<Order> findByUserId(UUID userId, Pageable pageable);
//
//    Page<Order> findByBusinessId(UUID businessId, Pageable pageable);
//
//    Page<Order> findByRiderId(UUID riderId, Pageable pageable);
//
//    Page<Order> findByBusinessIdAndStatus(UUID businessId, OrderStatus status, Pageable pageable);
//
//    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);
    Optional<Order> findById(UUID id);

    @Query("SELECT o FROM Order o WHERE o.business.id = :businessId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findRecentByBusinessAndStatus(@Param("businessId") UUID businessId,
                                              @Param("status") OrderStatus status,
                                              Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.business.id = :businessId AND o.status = :status")
    long countByBusinessAndStatus(@Param("businessId") UUID businessId, @Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.business.id = :businessId AND o.status = 'DELIVERED' AND o.createdAt BETWEEN :from AND :to")
    Optional<BigDecimal> sumRevenueForBusinessBetween(@Param("businessId") UUID businessId,
                                                      @Param("from") OffsetDateTime from,
                                                      @Param("to") OffsetDateTime to);

    @Query("""
    SELECT o.user
    FROM Order o
    WHERE o.business.id = :businessId
    ORDER BY o.createdAt DESC
""")
    List<User> findUsersByBusinessId(@Param("businessId") UUID businessId);

    List<Order> findByBusinessIdOrderByCreatedAtDesc(UUID businessId);
    @Query("""
    SELECT o FROM Order o
    WHERE o.pickupRider IS NULL
      AND o.status = 'PENDING'
    ORDER BY o.createdAt ASC
    """)
    List<Order> findAvailableForPickup();

    @Query("""
    SELECT o FROM Order o
    WHERE o.dropoffRider IS NULL
      AND o.status = 'READY'
    ORDER BY o.createdAt ASC
    """)
    List<Order> findAvailableForDropoff();

    boolean existsByIdAndPickupRiderId(UUID id, UUID pickupRider);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.items oi " +  // Changed from o.orderItems to o.items
            "WHERE o.business.id = :businessId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findOrdersWithItemsByBusinessId(@Param("businessId") UUID businessId);

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID accountId);
    @Query("SELECT o FROM Order o WHERE o.business.id = :businessId AND o.status = 'CONFIRMED' AND o.pickupRider IS NULL")
    List<Order> findAvailableForPickupByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT o FROM Order o WHERE o.business.id = :businessId AND o.status = 'READY' AND o.dropoffRider IS NULL")
    List<Order> findAvailableForDropoffByBusinessId(@Param("businessId") UUID businessId);
}

