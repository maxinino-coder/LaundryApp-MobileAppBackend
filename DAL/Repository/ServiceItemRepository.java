package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

    List<ServiceItem> findByBusinessId(UUID businessId);

    List<ServiceItem> findByBusinessAccountId(UUID accountId);

    Optional<ServiceItem> findByBusinessIdAndItemKey(UUID businessId, String itemKey);

    Optional<ServiceItem> findByBusinessAccountIdAndItemKey(UUID accountId, String itemKey);

    void deleteByBusinessId(UUID businessId);
}
