package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Enum.OrderStatus;
import jakarta.persistence.criteria.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Page<Order> findByBusinessId(UUID businessId, Pageable pageable);
    Page<Order> findByRiderId(UUID riderId, Pageable pageable);

    Page<Order> findByBusinessIdAndStatus(UUID businessId, OrderStatus status, Pageable pageable);
    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

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
}

