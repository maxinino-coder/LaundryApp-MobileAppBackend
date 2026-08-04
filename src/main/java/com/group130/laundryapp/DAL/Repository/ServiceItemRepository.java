package com.group130.laundryapp.DAL.Repository;

import com.group130.laundryapp.Domain.Entity.ServiceItem;
import com.group130.laundryapp.Domain.Enum.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

    List<ServiceItem> findByBusinessIdAndIsActiveTrue(UUID businessId);

    @Query("SELECT s FROM ServiceItem s WHERE s.business.id = :businessId AND s.category = :category AND s.isActive = true")
    List<ServiceItem> findByBusinessAndCategory(
            @Param("businessId") UUID businessId,
            @Param("category") ServiceCategory category);
}
