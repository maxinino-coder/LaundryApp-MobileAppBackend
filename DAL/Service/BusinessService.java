package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.*;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
//import com.group130.laundryapp.laundry2_0.Domain.Entity.ServiceItem;
import com.group130.laundryapp.laundry2_0.Domain.Enum.AccountRole;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.SettlementStatus;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group130.laundryapp.laundry2_0.DAL.Repository.ServiceItemRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.ServiceItem;
import com.group130.laundryapp.laundry2_0.Domain.DTO.ServiceItemDTO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final BusinessRepository businessRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BusinessPayoutRepository businessPayoutRepository;
    private final RiderRepository riderRepository;
    private final ServiceItemRepository serviceItemRepository;

    // Overload 1: Called by /business_info
    public Business getBusinessByAccountId(UUID id) {
        return businessRepository.findByAccountId(id)
                .or(() -> businessRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Business not found for ID: " + id));
    }

    // Overload 2: Called by /getbusinessInfo
    public List<businessInfo> getBusinessInfo(String name, String location) {
        return getBusinessInfo(name, location, null);
    }

    // Overload 3: The MAIN logic with all 3 params (all nullable)
    public List<businessInfo> getBusinessInfo(
            @Nullable String name,
            @Nullable String location,
            @Nullable UUID accountId) {

        // Normalize String inputs
        boolean isNameEmpty = (name == null || name.trim().isEmpty());
        boolean isLocationEmpty = (location == null || location.trim().isEmpty());

        // FIXED: UUID is an object, just check if it's null (no trim() or isEmpty())
        boolean isAccountIdProvided = (accountId != null);

        List<Business> businesses;

        // If accountId is provided, use it exclusively (ignore name/location)
        if (isAccountIdProvided) {
            // FIXED: accountId is already a UUID, use it directly
            Business business = businessRepository.findByAccountId(accountId)
                    .orElseThrow(() -> new RuntimeException("Business not found for accountId: " + accountId));
            businesses = List.of(business);
        } else {
            // Original logic for name/location
            if (isNameEmpty && isLocationEmpty) {
                businesses = businessRepository.findAll();
            } else if (!isNameEmpty && isLocationEmpty) {
                businesses = businessRepository.findByBusinessNameContainingIgnoreCase(name);
            } else if (isNameEmpty) {
                businesses = businessRepository.findByAddressContainingIgnoreCase(location);
            } else {
                businesses = businessRepository.findByBusinessNameContainingIgnoreCaseAndAddressContainingIgnoreCase(name, location);
            }
        }

        // Map to DTO with null-safe defaults
        return businesses.stream()
                .map(business -> businessInfo.builder()
                        .Id(business.getId())
                        .BusinessName(nullSafeString(business.getBusinessName()))
                        .Description(nullSafeString(business.getDescription()))
                        .LogoUrl(nullSafeString(business.getLogoUrl()))
                        .BannerUrl(nullSafeString(business.getBannerUrl()))
                        .Address(nullSafeString(business.getAddress()))
                        .City(nullSafeString(business.getCity()))
                        .Latitude(nullSafeDouble(business.getLatitude()))
                        .Longitude(nullSafeDouble(business.getLongitude()))
                        .OpeningTime(business.getOpeningTime())
                        .ClosingTime(business.getClosingTime())
                        .BankName(nullSafeString(business.getBankName()))
                        .BankAccountNo(nullSafeString(business.getBankAccountNo()))
                        .BankAccountName(nullSafeString(business.getBankAccountName()))
                        .MomoNumber(nullSafeString(business.getMomoNumber()))
                        .PricingModel(business.getPricingModel() != null ? business.getPricingModel() : com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel.PER_ITEM)
                        .PricePerKg(business.getPricePerKg() != null ? business.getPricePerKg() : new java.math.BigDecimal("15.00"))
                        .build())
                .collect(Collectors.toList());
    }

    // Helper methods for null-safety
    private String nullSafeString(String value) {
        return value != null ? value : "";
    }

    private Double nullSafeDouble(Double value) {
        return value != null ? value : 0.0;
    }
    public List<userInfo> getUsersByBusinessPurchased(UUID accountId) {
        // Get business using accountId
        Business business = businessRepository.findByAccountId(accountId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Business not found"
                                ));
        // Get users that ordered
        var users = orderRepository.findUsersByBusinessId(business.getId());
        // Map to DTO
        return users.stream()
                .map(user -> userInfo.builder().firstName(
                                        user.getFirstName())
                                .lastName(user.getLastName())
                                .Address(user.getAddress())
                                .City(user.getCity())
                                .latitude(user.getLatitude())
                                .longitude(user.getLongitude())
                                .build()
                )
                .toList();
    }

//    public List<orderInfo> getBusinesOrders(UUID accountId) {
//        Business business = businessRepository.findByAccountId(accountId)
//                .orElseThrow(() ->
//                    new RuntimeException(
//                            "Business not found"
//                    ));
//        var orders = orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId());
//        return orders.stream()
//                .map(order -> orderInfo.builder()
//                        .OrderNumber(order.getOrderNumber())
//                        .UserId(order.getUser() != null
//                                        ? order.getUser().getId()
//                                        : null)
//                        .BusinessId(order.getBusiness() != null
//                                        ? order.getBusiness().getId()
//                                        : null)
//                        .PickUpRiderId(order.getPickupRider() != null
//                                        ? order.getPickupRider().getId()
//                                        : null)
//                        .DelivaryRiderId(order.getDropoffRider() != null
//                                ? order.getDropoffRider().getId()
//                                : null)
//                        .Status(order.getStatus())
//                        .PickupAddress(order.getPickupAddress())
//                        .PickupLat(order.getPickupLat())
//                        .PickupLng(order.getPickupLng())
//                        .PickupTime(order.getPickupTime())
//                        .delivery_address(order.getDeliveryAddress())
//                        .DeliveryLat(order.getDeliveryLat())
//                        .DeliveryLng(order.getDeliveryLng())
//                        .DeliveryTime(order.getDeliveryTime())
//                        .Subtotal(order.getSubtotal() != null
//                                        ? order.getSubtotal().doubleValue()
//                                        : 0.0)
//                        .DeliveryFee(order.getDeliveryFee() != null
//                                        ? order.getDeliveryFee().doubleValue()
//                                        : 0.0)
//                        .DiscountAmount(order.getDiscountAmount() != null
//                                        ? order.getDiscountAmount().doubleValue()
//                                        : 0.0)
//                        .TotalAmount(order.getTotalAmount() != null
//                                        ? order.getTotalAmount().doubleValue()
//                                        : 0.0)
//                        .Notes(order.getNotes())
//                        .CancelledReason(order.getCancelledReason())
//                        .CreatedAt(order.getCreatedAt())
//                        .UpdatedAt(order.getUpdatedAt())
//                        .build()
//                )
//                .toList();
//    }
public List<orderInfo> getBusinesOrders(UUID accountId) {
    Business business = businessRepository.findByAccountId(accountId)
            .orElseThrow(() -> new RuntimeException("Business not found"));

    var orders = orderRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId());

    return orders.stream()
            .map(order -> {
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
            })
            .collect(Collectors.toList());
}

    public Order updateOrderStatus(UUID orderId, OrderStatus orderStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(orderStatus);

        return orderRepository.save(order);
    }

    public Business updateBusinessProfile(UUID accountId, UpdateBusinessProfile request) {
        Business business = businessRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Business Account not found"));

        // Only update fields that are not null
        if (request.getBusiness_name() != null) {
            business.setBusinessName(request.getBusiness_name());
        }
        if (request.getDescription() != null) {
            business.setDescription(request.getDescription());
        }
        if (request.getLogo_url() != null) {
            business.setLogoUrl(request.getLogo_url());
        }
        if (request.getBanner_url() != null) {
            business.setBannerUrl(request.getBanner_url());
        }
        if (request.getAddress() != null) {
            business.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            business.setCity(request.getCity());
        }
        if (request.getLatitude() != null) {
            business.setLatitude(Double.valueOf(request.getLatitude()));
        }
        if (request.getLongitude() != null) {
            business.setLongitude(Double.valueOf(request.getLongitude()));
        }
        if (request.getOpening_time() != null) {
            business.setOpeningTime(request.getOpening_time());
        }
        if (request.getClosing_time() != null) {
            business.setClosingTime(request.getClosing_time());
        }
        if (request.getBank_name() != null) {
            business.setBankName(request.getBank_name());
        }
        if (request.getBank_account_no() != null) {
            business.setBankAccountNo(request.getBank_account_no());
        }
        if (request.getBank_account_name() != null) {
            business.setBankAccountName(request.getBank_account_name());
        }
        if (request.getMomo_number() != null) {
            business.setMomoNumber(request.getMomo_number());
        }
        if (request.getPricing_model() != null) {
            business.setPricingModel(request.getPricing_model());
        }
        if (request.getPrice_per_kg() != null) {
            try {
                business.setPricePerKg(new java.math.BigDecimal(request.getPrice_per_kg()));
            } catch (Exception ignored) {}
        }
        if (request.getCreated_at() != null) {
            business.setCreatedAt(request.getCreated_at());
        }
        if (request.getUpdated_at() != null) {
            business.setUpdatedAt(request.getUpdated_at());
        }
        if (request.getPaystack_subaccount_code() != null) {
            business.setPaystackSubaccountCode(request.getPaystack_subaccount_code());
        }

        return businessRepository.save(business);
    }

    public BusinessPayoutDTO getBusinessPayout(UUID accountId) {
        // Check if account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Check if account is a business
        if (account.getRole() != AccountRole.BUSINESS) {
            throw new RuntimeException("Account is not a business account");
        }

        // Get the business associated with the account
        Business business = businessRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Business not found for this account"));

        // Get all payouts for this business
        List<BusinessPayout> payouts = businessPayoutRepository.findByBusinessIdOrderByCreatedAtDesc(business.getId());

        // Calculate totals
        BigDecimal totalRevenue = payouts.stream()
                .map(BusinessPayout::getOrderRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = payouts.stream()
                .map(BusinessPayout::getPlatformCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRiderFee = payouts.stream()
                .map(BusinessPayout::getRiderFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNetPayout = payouts.stream()
                .map(BusinessPayout::getNetPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = payouts.stream()
                .filter(p -> p.getStatus() == SettlementStatus.PENDING)
                .count();

        long settledCount = payouts.stream()
                .filter(p -> p.getStatus() == SettlementStatus.SETTLED)
                .count();

        return BusinessPayoutDTO.builder()
                .businessId(business.getId())
                .businessName(business.getBusinessName())
                .totalRevenue(totalRevenue)
                .totalCommission(totalCommission)
                .totalRiderFee(totalRiderFee)
                .totalNetPayout(totalNetPayout)
                .pendingCount(pendingCount)
                .settledCount(settledCount)
                .build();
    }

    public List<riderInfo> approveRiderAccount(UUID accountId, Boolean choice) {
        Business business = businessRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Business Account not found"));

        List<Rider> riders = riderRepository.findByBusinessId(business.getId());
        if (riders.isEmpty()) {
            throw new RuntimeException("No riders found");
        }

        boolean approved = Boolean.TRUE.equals(choice);
        riders.forEach(r -> r.setApproved(approved));

        return riderRepository.saveAll(riders)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private riderInfo toDto(Rider rider) {
        return riderInfo.builder()
                .Id(rider.getId())
                .FirstName(rider.getFirstName())
                .IsApproved(rider.isApproved())
                .build();
    }

    public List<Rider> getRidersForBusniess(UUID accountId) {
        Business business = businessRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Business Account not found"));

        List<Rider> riders = riderRepository.findByBusinessId(business.getId());
        if (riders.isEmpty()) {
            throw new RuntimeException("No riders found");
        }

        return riders;
    }


//    @Transactional
    public List<ServiceItemDTO> getServiceItems(UUID accountOrBusinessId) {
        Business business = getBusinessByAccountId(accountOrBusinessId);
        List<ServiceItem> items = serviceItemRepository.findByBusinessId(business.getId());
        return items.stream()
                .map(i -> ServiceItemDTO.builder()
                        .id(i.getId())
                        .itemKey(i.getItemKey())
                        .name(i.getName())
                        .unitPrice(i.getUnitPrice())
                        .icon(i.getIcon())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ServiceItemDTO> updateServiceItems(UUID accountOrBusinessId, List<ServiceItemDTO> dtos) {
        Business business = getBusinessByAccountId(accountOrBusinessId);
        if (dtos == null || dtos.isEmpty()) {
            return getServiceItems(business.getId());
        }

        for (ServiceItemDTO dto : dtos) {
            if (dto.getItemKey() == null || dto.getItemKey().isBlank()) continue;
            ServiceItem item = serviceItemRepository
                    .findByBusinessIdAndItemKey(business.getId(), dto.getItemKey())
                    .orElse(ServiceItem.builder()
                            .business(business)
                            .itemKey(dto.getItemKey())
                            .name(dto.getName() != null ? dto.getName() : dto.getItemKey())
                            .build());

            if (dto.getName() != null) item.setName(dto.getName());
            if (dto.getUnitPrice() != null) item.setUnitPrice(dto.getUnitPrice());
            if (dto.getIcon() != null) item.setIcon(dto.getIcon());
            item.setActive(true);

            serviceItemRepository.save(item);
        }

        return getServiceItems(business.getId());
    }
}

