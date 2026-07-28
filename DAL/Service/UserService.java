package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.*;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;

import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserService {
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
//    private final ServiceItemRepository serviceItemRepository;

    public userInfo getUserInfo(UUID accountId) {
        User user = userRepository
                .findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("User not found with email " ));


        return userInfo.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .City(user.getCity())
                .Address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .email(user.getAccount() != null ? user.getAccount().getEmail() : null)
                .phone(user.getAccount() != null ? user.getAccount().getPhone() : null)
                .build();
    }

    @Transactional
    public orderInfo createBusinessOrder(CreateBusinessOrderRequest request) {
        // 1. Resolve the customer — the app sends the auth ACCOUNT id
        User user = userRepository.findByAccountId(request.getAccountId())
                .or(() -> userRepository.findById(request.getAccountId()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new RuntimeException("Business not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }
        if (request.getPickupAddress() == null || request.getPickupAddress().isBlank()) {
            throw new IllegalArgumentException("Pickup address is required.");
        }

        // delivery_address is NOT NULL in the schema — default to returning to the pickup point
        String deliveryAddress = (request.getDeliveryAddress() != null && !request.getDeliveryAddress().isBlank())
                ? request.getDeliveryAddress()
                : request.getPickupAddress();
        Double deliveryLat = request.getDeliveryLat() != null ? request.getDeliveryLat() : request.getPickupLat();
        Double deliveryLng = request.getDeliveryLng() != null ? request.getDeliveryLng() : request.getPickupLng();

        // 2. Price the items using provided unitPrice or category table, and compute totals
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemsRequest itemRequest : request.getItems()) {
            int qty = itemRequest.getQuantity() <= 0 ? 1 : itemRequest.getQuantity();
            BigDecimal unitPrice = (itemRequest.getUnitPrice() != null && itemRequest.getUnitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? itemRequest.getUnitPrice()
                    : ServicePricing.priceFor(itemRequest.getServiceCategory());
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal pickupFee  = ServicePricing.PICKUP_FEE;
        BigDecimal dropoffFee = ServicePricing.DROPOFF_FEE;
        BigDecimal totalAmount = subtotal.add(pickupFee).add(dropoffFee);

        // 3. Build and save the Order parent entity
        Order order = Order.builder()
                .user(user)
                .business(business)
                .orderNumber(generateOrderNumber())
                .pickupAddress(request.getPickupAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .deliveryAddress(deliveryAddress)
                .deliveryLat(deliveryLat)
                .deliveryLng(deliveryLng)
                .notes(request.getNotes())
                .subtotal(subtotal)
                .pickupFee(pickupFee)
                .dropoffFee(dropoffFee)
                .deliveryFee(pickupFee.add(dropoffFee))
                .totalAmount(totalAmount)
                .createdAt(OffsetDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 4. Persist the item lines (price snapshotted at order time)
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        for (OrderItemsRequest itemRequest : request.getItems()) {
            int qty = itemRequest.getQuantity() <= 0 ? 1 : itemRequest.getQuantity();
            BigDecimal unitPrice = (itemRequest.getUnitPrice() != null && itemRequest.getUnitPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? itemRequest.getUnitPrice()
                    : ServicePricing.priceFor(itemRequest.getServiceCategory());

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .serviceCategory(itemRequest.getServiceCategory() != null ? itemRequest.getServiceCategory() : com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory.WASH_FOLD)
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .notes(itemRequest.getNote())
                    .build();
            orderItemRepository.save(orderItem);

            itemDTOs.add(OrderItemDTO.builder()
                    .serviceCategory(itemRequest.getServiceCategory() != null ? itemRequest.getServiceCategory() : com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory.WASH_FOLD)
                    .Quantity(qty)
                    .UnitPrice(unitPrice)
                    .LineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)))
                    .Notes(itemRequest.getNote())
                    .build());
        }

        return orderInfo.from(savedOrder, itemDTOs);
    }
    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD-" +
                date +
                "-" +
                random;

    }


    public User updateUserProfile(UUID accountId, UpdateUserProfile request) {
        User user = userRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("User Account not found"));

        // Update only fields that are not null
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        Account account = user.getAccount(); // assume never null
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }



        return userRepository.save(user);
    }
}
