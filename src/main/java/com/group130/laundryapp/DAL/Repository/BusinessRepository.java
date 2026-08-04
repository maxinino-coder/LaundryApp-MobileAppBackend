package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByAccountId(UUID accountId);
    List<Business>     findByIsApprovedTrue();
    List<Business> findByIsOpenTrueAndIsApprovedTrue();

    @Query("SELECT b FROM Business b WHERE b.isApproved = true AND b.isOpen = true AND b.city = :city")
    Page<Business> findOpenBusinessesInCity(@Param("city") String city, Pageable pageable);

    // Average rating via reviews
    @Query("SELECT COALESCE(AVG(r.businessRating), 0) FROM Review r WHERE r.business.id = :businessId AND r.isVisible = true")
    Double getAverageRating(@Param("businessId") UUID businessId);
}
