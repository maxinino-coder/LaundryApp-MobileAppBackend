package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiderRepository extends JpaRepository<Rider, UUID> {

    Optional<Rider> findByAccountId(UUID accountId);
    List<Rider>        findByBusinessId(UUID businessId);
    List<Rider>        findByBusinessIdAndIsAvailableTrue(UUID businessId);

    @Query("SELECT r FROM Rider r WHERE r.business.id = :businessId AND r.isApproved = true AND r.isAvailable = true")
    List<Rider> findAvailableRidersForBusiness(@Param("businessId") UUID businessId);
}

