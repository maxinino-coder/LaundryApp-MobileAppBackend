package com.group130.laundryapp.laundry2_0.DAL.Repository;

import com.group130.laundryapp.laundry2_0.Domain.Entity.RidesAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RidesAssignmentRepository extends JpaRepository<RidesAssignment, UUID> {

    List<RidesAssignment> findByOrderId(UUID orderId);
}
