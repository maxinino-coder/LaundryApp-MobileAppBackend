package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.OrderItemRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.OrderRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.RiderEarningRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.RidesAssignmentRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.UserRepository;
import com.group130.laundryapp.laundry2_0.Domain.DTO.OrderItemDTO;
import com.group130.laundryapp.laundry2_0.Domain.DTO.RideApplicantDTO;
import com.group130.laundryapp.laundry2_0.Domain.DTO.orderInfo;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final RidesAssignmentRepository ridesAssignmentRepository;
    private final RiderPayoutService riderPayoutService;
    private final RiderEarningRepository riderEarningRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 2. USER CALL: Review applicants and accept one rider.
     * accountId is the user's auth account id — resolved to the User row here.
     */
    @Transactional
    public orderInfo userAcceptRider(UUID orderId, UUID accountId, UUID applicationId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order context not found."));

        User user = userRepository.findByAccountId(accountId)
                .or(() -> userRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("User not found."));

        // SECURITY CHECK: Verify the requester actually owns and pays for this order
        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }

        RidesAssignment targetApplication = ridesAssignmentRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application submission not found."));

        if (!targetApplication.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("This application does not belong to the specified order.");
        }

        // Assign as pickup rider first; if pickup is taken, this applicant does the dropoff leg
        PaymentType leg;
        BigDecimal legFee;
        if (order.getPickupRider() == null) {
            order.setPickupRider(targetApplication.getRider());
            leg = PaymentType.RIDE_PICKUP;
            legFee = order.getPickupFee();
            order.setStatus(OrderStatus.ACCEPTED); // out of the open pickup pool
        } else {
            order.setDropoffRider(targetApplication.getRider());
            leg = PaymentType.RIDE_DROPOFF;
            legFee = order.getDropoffFee();
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY); // dropoff rider committed
        }

        Order savedOrder = orderRepository.save(order);

        // The payout service requires a RiderEarning row per leg — create it now,
        // at the moment the rider is committed to the leg.
        if (riderEarningRepository.findByOrderIdAndLeg(orderId, leg).isEmpty()) {
            riderEarningRepository.save(RiderEarning.builder()
                    .rider(targetApplication.getRider())
                    .order(savedOrder)
                    .leg(leg)
                    .amount(legFee != null ? legFee : BigDecimal.ZERO)
                    .build());
        }
        // Clear out the application statuses cleanly
        List<RidesAssignment> allApplications = ridesAssignmentRepository.findByOrderId(orderId);
        for (RidesAssignment app : allApplications) {
            if (app.getId().equals(applicationId)) {
                app.setStatus("ACCEPTED");
            } else {
                app.setStatus("REJECTED");
            }
        }
        ridesAssignmentRepository.saveAll(allApplications);

        return toOrderInfo(savedOrder);
    }

    /**
     * USER CONFIRMS RIDER: After accepting an applicant, the user explicitly
     * confirms the rider. This moves the order to CONFIRMED status, which
     * is the signal for the rider's route map to become visible.
     * POST /api/v1/users/{accountId}/orders/{orderId}/confirm-rider
     *
     */

    /**
     * 2b. USER CALL: Decline a rider's application without accepting anyone else.
     * Leaves the order open for other applicants — does not touch order status
     * or pickup/dropoff rider assignment.
     */
    @Transactional
    public orderInfo userRejectRider(UUID orderId, UUID accountId, UUID applicationId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        User user = userRepository.findByAccountId(accountId)
                .or(() -> userRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }

        RidesAssignment targetApplication = ridesAssignmentRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application submission not found."));

        if (!targetApplication.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("This application does not belong to the specified order.");
        }

        if ("ACCEPTED".equals(targetApplication.getStatus())) {
            throw new IllegalStateException("Cannot reject an application that has already been accepted.");
        }

        targetApplication.setStatus("REJECTED");
        ridesAssignmentRepository.save(targetApplication);

        return toOrderInfo(order);
    }
    @Transactional
    public orderInfo userConfirmRider(UUID orderId, UUID accountId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found."));

        User user = userRepository.findByAccountId(accountId)
                .or(() -> userRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not own this order.");
        }

        if (order.getPickupRider() == null) {
            throw new IllegalStateException("No rider has been assigned to this order yet.");
        }

        // Move to CONFIRMED — this is what the rider's route page checks
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);
        return toOrderInfo(saved);
    }


    /**
     * Users view who has applied to pick up their order.
     * accountId is the user's auth account id.
     */
    @Transactional
    public List<RideApplicantDTO> getOrderApplicants(UUID orderId, UUID accountId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        User user = userRepository.findByAccountId(accountId)
                .or(() -> userRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to order applicant list.");
        }

        return ridesAssignmentRepository.findByOrderId(orderId).stream()
                .map(a -> RideApplicantDTO.builder()
                        .applicationId(a.getId())
                        .orderId(orderId)
                        .riderId(a.getRider().getId())
                        .riderFirstName(a.getRider().getFirstName())
                        .riderLastName(a.getRider().getLastName())
                        .vehicleType(a.getRider().getVehicleType() != null
                                ? a.getRider().getVehicleType().name() : null)
                        .vehiclePlate(a.getRider().getVehiclePlate())
                        .status(a.getStatus())
                        .appliedAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Order completeOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryTime(OffsetDateTime.now());

        Rider rider = order.getDropoffRider();
        if (rider != null) {
            rider.setAvailable(true);

            List<RidesAssignment> assignments = ridesAssignmentRepository.findByOrderId(orderId);
            for (RidesAssignment assignment : assignments) {
                if ("ACCEPTED".equals(assignment.getStatus())) {
                    assignment.setStatus("COMPLETED");
                    ridesAssignmentRepository.save(assignment);
                }
            }
        }

        Order savedOrder = orderRepository.save(order);

        riderPayoutService.confirmDropoff(savedOrder);

        return savedOrder;
    }

    /** Orders for the customer — accountId is the user's auth account id. */
    public List<orderInfo> getOrdersById(UUID accountId) {
        User user = userRepository.findByAccountId(accountId)
                .or(() -> userRepository.findById(accountId))
                .orElseThrow(() -> new RuntimeException("User not found with id: " + accountId));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return orders.stream()
                .map(this::toOrderInfo)
                .collect(Collectors.toList());
    }

    private orderInfo toOrderInfo(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        List<OrderItemDTO> itemDTOs = orderItems.stream()
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
}
