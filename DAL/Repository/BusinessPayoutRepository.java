package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.BusinessPayout;
import com.group130.laundryapp.laundry2_0.Domain.Enum.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessPayoutRepository extends JpaRepository<BusinessPayout, UUID> {

    List<BusinessPayout> findByBusinessId(UUID businessId);
    List<BusinessPayout> findByBusinessIdAndStatus(UUID businessId, SettlementStatus status);

    @Query("SELECT SUM(bp.netPayout) FROM BusinessPayout bp WHERE bp.business.id = :businessId AND bp.status = 'SETTLED'")
    Optional<BigDecimal> sumSettledPayoutsByBusiness(@Param("businessId") UUID businessId);

    List<BusinessPayout> findByBusinessIdOrderByCreatedAtDesc(UUID id);
}

