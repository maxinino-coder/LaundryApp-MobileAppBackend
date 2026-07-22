package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.RiderEarning;
import com.group130.laundryapp.laundry2_0.Domain.Enum.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;


@Repository
public interface RiderEarningRepository extends JpaRepository<RiderEarning, UUID> {

    List<RiderEarning> findByRiderId(UUID riderId);
    List<RiderEarning> findByRiderIdAndStatus(UUID riderId, SettlementStatus status);

    @Query("SELECT SUM(re.amount) FROM RiderEarning re WHERE re.rider.id = :riderId AND re.status = 'SETTLED'")
    Optional<BigDecimal> sumSettledEarningsByRider(@Param("riderId") UUID riderId);

    @Query("SELECT SUM(re.amount) FROM RiderEarning re WHERE re.rider.id = :riderId AND re.status = 'PENDING'")
    Optional<BigDecimal> sumPendingEarningsByRider(@Param("riderId") UUID riderId);

    Optional<RiderEarning> findByOrderIdAndLeg(UUID orderId, PaymentType leg);


    List<RiderEarning> findByRiderIdOrderByCreatedAtDesc(UUID id);
}
