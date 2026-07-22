package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByAccountId(UUID accountId);

    // Uncomment or remove this method
    @Query("SELECT COALESCE(AVG(r.businessRating), 0) FROM Review r WHERE r.business.id = :businessId AND r.isVisible = true")
    Double getAverageRating(@Param("businessId") UUID businessId);

    List<Business> findByAddressContainingIgnoreCase(String address);
    List<Business> findByBusinessNameContainingIgnoreCase(String businessName);
    List<Business> findByBusinessNameContainingIgnoreCaseAndAddressContainingIgnoreCase(String businessName, String location);
}