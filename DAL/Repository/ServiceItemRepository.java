//package com.group130.laundryapp.laundry2_0.DAL.Repository;
//
//import com.group130.laundryapp.laundry2_0.Domain.Entity.ServiceItem;
//import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.UUID;
//
//@Repository
//public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
//
//    List<ServiceItem> findByBusinessIdAndIsActiveTrue(UUID businessId);
//
//    @Query("SELECT s FROM ServiceItem s WHERE s.business.id = :businessId AND s.category = :category AND s.isActive = true")
//    List<ServiceItem> findByBusinessAndCategory(
//            @Param("businessId") UUID businessId,
//            @Param("category") ServiceCategory category);
//
//    boolean existsByBusinessIdAndNameIgnoreCase(UUID businessId, String name);
//}
