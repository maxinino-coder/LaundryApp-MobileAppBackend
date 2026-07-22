package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.google.api.client.util.DateTime;
import com.group130.laundryapp.laundry2_0.DAL.Repository.*;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RiderService {
    private final RiderRepository riderRepository;
    private final OrderRepository orderRepository;
    private final RidesAssignmentRepository ridesAssignmentRepository;
    private final AccountRepository accountRepository;
    private final RiderEarningRepository riderEarningRepository;
    private final RiderPayoutService riderPayoutService;

    public List<riderInfo> getRiderInfo(){
        List<Rider> riders = riderRepository.findAll();
        return riders.stream().map(
                rider -> riderInfo.builder()
                        .FirstName(rider.getFirstName())
                        .LastName(rider.getLastName())
                        .AvatarUrl(rider.getAvatarUrl())
                        .VehicleType(rider.getVehicleType())
                        .RiderType(rider.getRiderType())
                        .VehiclePlate(rider.getVehiclePlate())
                        .IsApproved(rider.isApproved())
                        .IsAvailable(rider.isAvailable())
                        .CurrentLat(rider.getCurrentLat())
                        .CurrentLng(rider.getCurrentLng())
                        .LastLocationAt(rider.getCurrentLat())
                        .MomoNumber(rider.getMomoNumber())
                        .BankAccountNo(rider.getBankAccountNo())
                        .CreatedAt(rider.getCreatedAt())
                        .UpdatedAt(rider.getUpdatedAt())
                        .build()
        ).toList();
    }

    public List<availableOrderInfo> getAvailableOrders() {
        // Pickup legs (CONFIRMED, no pickup rider) + dropoff legs (READY, no dropoff rider)
        List<Order> orders = new java.util.ArrayList<>(orderRepository.findAvailableForPickup());
        orders.addAll(orderRepository.findAvailableForDropoff());

        return orders.stream()
                .map(order -> availableOrderInfo.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .status(order.getStatus())
                        .businessName(order.getBusiness().getBusinessName())
                        .pickupAddress(order.getPickupAddress())
                        .pickupLat(order.getPickupLat())
                        .pickupLng(order.getPickupLng())
                        .deliveryAddress(order.getDeliveryAddress())
                        .deliveryLat(order.getDeliveryLat())
                        .deliveryLng(order.getDeliveryLng())
                        .deliveryFee(order.getDeliveryFee())
                        .createdAt(order.getCreatedAt())
                        .pickupTime(order.getPickupTime())
                        .build())
                .toList();
    }

    @Transactional
    public riderInfo getRiderProfile(UUID accountId) {
        // Frontend sends the account id (from SecureStore "account_id"), not the Rider row id.
        // Use findByAccountId which looks up by the linked Account foreign key.
        Rider rider = riderRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Rider profile not found."));

        return riderInfo.builder()
                .Id(rider.getId())
                .FirstName(rider.getFirstName())
                .LastName(rider.getLastName())
                .AvatarUrl(rider.getAvatarUrl())
                .VehicleType(rider.getVehicleType())
                .RiderType(rider.getRiderType())
                .VehiclePlate(rider.getVehiclePlate())
                .IsApproved(rider.isApproved())
                .IsAvailable(rider.isAvailable())
                .CurrentLat(rider.getCurrentLat())
                .CurrentLng(rider.getCurrentLng())
                .LastLocationAt(rider.getCurrentLat())
                .MomoNumber(rider.getMomoNumber())
                .BankAccountNo(rider.getBankAccountNo())
                .CreatedAt(rider.getCreatedAt())
                .UpdatedAt(rider.getUpdatedAt())
                .build();
    }
    /**
     * 1. RIDER CALL: Choose/Apply for a ride assignment.
     * The id sent by the app is the rider's ACCOUNT id — resolve it first,
     * falling back to a direct Rider row id for backwards compatibility.
     */
    @Transactional
    public RidesAssignment riderApplyForOrder(UUID riderOrAccountId, UUID orderId) {
        Rider rider = riderRepository.findByAccountId(riderOrAccountId)
                .or(() -> riderRepository.findById(riderOrAccountId))
                .orElseThrow(() -> new RuntimeException("Rider profile not found."));

        if (!rider.isApproved() || !rider.isAvailable()) {
            throw new IllegalStateException("Rider must be approved and marked active/available.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        Business business = order.getBusiness();
        if (business == null) {
            throw new RuntimeException("Cannot apply for order: Order ID " + orderId + " is not associated with any business.");
        }

        // Open for applications while awaiting a pickup rider (PENDING/CONFIRMED)
        // or awaiting a dropoff rider (READY)
        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("This order is no longer open for fulfillment application.");
        }

        boolean alreadyApplied = ridesAssignmentRepository.findByOrderId(orderId).stream()
                .anyMatch(a -> a.getRider().getId().equals(rider.getId()));
        if (alreadyApplied) {
            throw new IllegalStateException("You have already applied to pick up this order.");
        }

        RidesAssignment application = RidesAssignment.builder()
                .order(order)
                .rider(rider)
                .business(order.getBusiness()) // <-- ADD THIS LINE
                .status("PENDING")
                .build();

        return ridesAssignmentRepository.save(application);
    }

    /**
     * Rider confirms goods collected from the customer.
     * Advances the order and triggers the pickup-leg payout attempt.
     */
    @Transactional
    public void confirmPickup(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        order.setStatus(OrderStatus.PICKED_UP);
        order.setPickupConfirmedAt(OffsetDateTime.now());
        Order saved = orderRepository.save(order);

        riderPayoutService.confirmPickup(saved);
    }

    /**
     * Rider confirms goods delivered to the business (laundry can begin).
     */
    @Transactional
    public void confirmDelivery(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setDropoffConfirmedAt(OffsetDateTime.now());
        orderRepository.save(order);
    }

    public RiderPayoutDTO getRiderPayout(UUID accountId) {
        // Validate account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getRole() != AccountRole.RIDER) {
            throw new RuntimeException("Account is not a rider account");
        }

        // Get rider
        Rider rider = riderRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Rider not found for this account"));

        // Get all earnings for this rider
        List<RiderEarning> earnings = riderEarningRepository
                .findByRiderIdOrderByCreatedAtDesc(rider.getId());

        // Calculate totals
        BigDecimal totalEarnings = earnings.stream()
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPending = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.PENDING)
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSettled = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.SETTLED)
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFailed = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.FAILED)
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Count by status
        long pendingCount = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.PENDING)
                .count();

        long settledCount = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.SETTLED)
                .count();

        long failedCount = earnings.stream()
                .filter(e -> e.getStatus() == RiderEarning.SettlementStatus.FAILED)
                .count();

        // Calculate by leg type
        BigDecimal pickupEarnings = earnings.stream()
                .filter(e -> e.getLeg() == PaymentType.RIDE_PICKUP)
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dropoffEarnings = earnings.stream()
                .filter(e -> e.getLeg() == PaymentType.RIDE_DROPOFF)
                .map(RiderEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RiderPayoutDTO.builder()
                .riderID(rider.getId())
                .riderFirstName(rider.getFirstName())
                .riderLastName(rider.getLastName())
                .riderEmail(rider.getAccount().getEmail())
                .riderPhone(rider.getMomoNumber())
                .totalEarnings(totalEarnings)
                .totalPending(totalPending)
                .totalSettled(totalSettled)
                .totalFailed(totalFailed)
                .pendingCount(pendingCount)
                .settledCount(settledCount)
                .failedCount(failedCount)
                .totalOrders((long) earnings.size())
                .pickupEarnings(pickupEarnings)
                .dropoffEarnings(dropoffEarnings)

                .build();
    }

    public Rider updateRiderProfile(UUID accountId, UpdateRiderProfile request) {
        Rider rider = riderRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Business Account not found"));

        // Only update fields that are not null
        if (request.getFirst_name() != null) {
            rider.setFirstName(request.getFirst_name());
        }
        if (request.getLast_name() != null) {
            rider.setLastName(request.getLast_name());
        }
        if (request.getAvatar_url() != null) {
            rider.setAvatarUrl(request.getAvatar_url());
        }
        if (request.getVehicle_type() != null) {
            rider.setVehicleType(request.getVehicle_type());
        }
        if (request.getVehicle_plate() != null) {
            rider.setVehiclePlate(request.getVehicle_plate());
        }
        if (request.getIs_available() != null) {
            rider.setAvailable(request.getIs_available());
        }
        if (request.getIs_approved() != null) {
            rider.setApproved(request.getIs_approved());
        }
        if (request.getCurrent_lat() != null) {
            rider.setCurrentLat(Double.parseDouble(request.getCurrent_lat()));
        }
        if (request.getCurrent_lng() != null) {
            rider.setCurrentLng(Double.parseDouble(request.getCurrent_lng()));
        }
        if (request.getLast_location_at() != null) {
            rider.setLastLocationAt(OffsetDateTime.parse(request.getLast_location_at()));
        }
        if (request.getMomo_number() != null) {
            rider.setMomoNumber(request.getMomo_number());
        }
        if (request.getBank_account_no() != null) {
            rider.setBankAccountNo(request.getBank_account_no());
        }
        if (request.getRider_type() != null) {
            rider.setRiderType(request.getRider_type());
        }
        // Always update the updated_at timestamp when updating profile
        rider.setUpdatedAt(OffsetDateTime.now());

        return riderRepository.save(rider);
    }

//


}
