package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.*;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class RiderService {
    private final RiderRepository riderRepository;
    private final OrderRepository orderRepository;
    private final RidesAssignmentRepository ridesAssignmentRepository;
    private final AccountRepository accountRepository;
    private final RiderEarningRepository riderEarningRepository;
    private final RiderPayoutService riderPayoutService;
    private final OrderItemRepository orderItemRepository;

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

    public List<availableOrderInfo> getAvailableOrders(UUID accountId) {
        List<Order> pickupOrders;
        List<Order> dropoffOrders;
        Rider rider = null;

        if (accountId != null) {
            rider = riderRepository.findByAccountId(accountId)
                    .or(() -> riderRepository.findById(accountId))
                    .orElse(null);
        }

        if (rider != null && Boolean.TRUE.equals(rider.isApproved()) && rider.getBusiness() != null) {
            UUID businessId = rider.getBusiness().getId();
            pickupOrders = orderRepository.findAvailableForPickupByBusinessId(businessId);
            dropoffOrders = orderRepository.findAvailableForDropoffByBusinessId(businessId);
        } else {
            pickupOrders = orderRepository.findAvailableForPickup();
            dropoffOrders = orderRepository.findAvailableForDropoff();
        }
        if (pickupOrders == null) pickupOrders = new ArrayList<>();
        if (dropoffOrders == null) dropoffOrders = new ArrayList<>();

        List<availableOrderInfo> result = new ArrayList<>();
        pickupOrders.forEach(o -> result.add(toAvailableOrderInfo(o, "PICKUP")));
        dropoffOrders.forEach(o -> result.add(toAvailableOrderInfo(o, "DELIVERY")));

        result.sort(Comparator.comparing(availableOrderInfo::getCreatedAt).reversed());
        return result;
    }

    // Helper method to map Order → availableOrderInfo
    private availableOrderInfo toAvailableOrderInfo(Order order, String jobType) {
        Business business = order.getBusiness();
        return availableOrderInfo.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .businessName(business != null ? business.getBusinessName() : null)
                .businessAddress(business != null ? business.getAddress() : null)
                .businessLat(business != null ? business.getLatitude() : null)
                .businessLng(business != null ? business.getLongitude() : null)
                .pickupAddress(order.getPickupAddress())
                .pickupLat(order.getPickupLat())
                .pickupLng(order.getPickupLng())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLat(order.getDeliveryLat())
                .deliveryLng(order.getDeliveryLng())
                .deliveryFee(order.getDeliveryFee())
                .createdAt(order.getCreatedAt())
                .pickupTime(order.getPickupTime())
                .jobType(jobType)
                .build();
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
    public RidesAssignmentResponseDTO riderApplyForOrder(UUID riderOrAccountId, UUID orderId) {
        Rider rider = riderRepository.findByAccountId(riderOrAccountId)
                .or(() -> riderRepository.findById(riderOrAccountId))
                .orElseThrow(() -> new RuntimeException("Rider profile not found."));

//        if (!(rider.isApproved() && rider.isAvailable())) {
//            throw new IllegalStateException("Rider must be approved and marked active/available.");
//        }

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

        // In your service method:
        RidesAssignment application = RidesAssignment.builder()
                .order(order)
                .rider(rider)
                .business(order.getBusiness())
                .status("PENDING")
                .build();

        RidesAssignment savedEntity = ridesAssignmentRepository.save(application);

        return convertToDto(savedEntity);
    }
    private RidesAssignmentResponseDTO convertToDto(RidesAssignment entity) {
        // Assuming your DTO has a builder (or you can use a constructor)
        return RidesAssignmentResponseDTO.builder()
                .id(entity.getId())
                .orderId(entity.getOrder().getId())      // only the ID, not the whole Order
                .riderId(entity.getRider().getId())      // only the ID
                .businessId(entity.getBusiness().getId())// only the ID
                .status(entity.getStatus())
                // Add any other primitive/immutable fields you need
                .build();
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

    /**
     * Returns orders where this rider is the active pickup or dropoff rider
     * and the order is not yet completed or cancelled.
     * Called by GET /api/v1/riders/{accountId}/active_jobs
     */
    @Transactional
    public List<orderInfo> getActiveJobs(UUID accountId) {
        Rider rider = riderRepository.findByAccountId(accountId)
                .or(() -> riderRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("Rider profile not found."));

        List<Order> activeOrders = orderRepository.findActiveJobsByRiderId(rider.getId());

        return activeOrders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    List<OrderItemDTO> itemDTOs = items.stream()
                            .map(item -> OrderItemDTO.builder()
                                    .Quantity(item.getQuantity())
                                    .UnitPrice(item.getUnitPrice())
                                    .LineTotal(BigDecimal.valueOf(item.getQuantity()).multiply(item.getUnitPrice()))
                                    .serviceCategory(item.getServiceCategory())
                                    .Notes(item.getNotes())
                                    .CreatedAt(item.getCreatedAt())
                                    .build())
                            .collect(Collectors.toList());

                    orderInfo info = orderInfo.from(order, itemDTOs);
                    boolean isDropoffLeg = order.getDropoffRider() != null
                            && order.getDropoffRider().getId().equals(rider.getId());
                    info.setJobType(isDropoffLeg ? "DELIVERY" : "PICKUP");
                    return info;
                })
                .collect(Collectors.toList());
    }

    public void updateRiderLocation(UUID accountId, Double lat, Double lng) {
        Rider rider = riderRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Rider profile not found for account " + accountId));
        rider.setCurrentLat(lat);
        rider.setCurrentLng(lng);
        rider.setLastLocationAt(OffsetDateTime.now());
        riderRepository.save(rider);
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
    /**
     * Authoritative check used by the rider's route screen: confirms this rider
     * is actually the pickup or dropoff rider on this order right now, and that
     * the order is still active. Throws if not — used to detect rejection,
     * reassignment, or cancellation while the rider is viewing the route.
     */
    @Transactional
    public orderInfo getJobStatus(UUID accountId, UUID orderId) {
        Rider rider = riderRepository.findByAccountId(accountId)
                .or(() -> riderRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("Rider profile not found."));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        boolean isPickupRider = order.getPickupRider() != null
                && order.getPickupRider().getId().equals(rider.getId());
        boolean isDropoffRider = order.getDropoffRider() != null
                && order.getDropoffRider().getId().equals(rider.getId());

        if (!isPickupRider && !isDropoffRider) {
            throw new SecurityException("You are not assigned to this order.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("This job is no longer active.");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemDTO> itemDTOs = items.stream()
                .map(item -> OrderItemDTO.builder()
                        .Quantity(item.getQuantity())
                        .UnitPrice(item.getUnitPrice())
                        .LineTotal(BigDecimal.valueOf(item.getQuantity()).multiply(item.getUnitPrice()))
                        .serviceCategory(item.getServiceCategory())
                        .Notes(item.getNotes())
                        .CreatedAt(item.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return orderInfo.from(order, itemDTOs);
    }

//


}
