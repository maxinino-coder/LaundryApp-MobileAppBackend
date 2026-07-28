package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class orderInfo {
     private UUID OrderId;
     private String  OrderNumber;
     private UUID UserId;
     private UUID BusinessId;
     private UUID PickUpRiderId;
    private UUID DelivaryRiderId;
     private OrderStatus Status;
     private String PickupAddress;
     private Double  PickupLat;
     private Double PickupLng;
     private OffsetDateTime PickupTime;
     private String    DeliveryAddress;
     private Double DeliveryLat;
    private Double DeliveryLng;
    private OffsetDateTime DeliveryTime;
    private Double Subtotal;
    private Double PickUpFee;
    private Double DropoffFee;
    private Double DeliveryFee;
    private Double DiscountAmount;
    private Double TotalAmount;
    private String Notes;
    private String CancelledReason;
    private OffsetDateTime CreatedAt;
    private OffsetDateTime UpdatedAt;
    private String BusinessName;
    private String BusinessAddress;
    private Double BusinessLat;
    private Double BusinessLng;
    private Double ActiveRiderLat;
    private Double ActiveRiderLng;
    private Boolean RiderActive;
    /** Not set by from() — caller fills this in per-rider (which leg they're on). */
    private String jobType;

    private List<OrderItemDTO> orderItems;

    /** Single mapping point Order → orderInfo so list/detail endpoints stay consistent. */
    public static orderInfo from(com.group130.laundryapp.laundry2_0.Domain.Entity.Order order,
                                 List<OrderItemDTO> itemDTOs) {
        com.group130.laundryapp.laundry2_0.Domain.Entity.Business business = order.getBusiness();

        Double riderLat = null;
        Double riderLng = null;
        boolean isRiderActive = false;

        // Rider marker only shows while job is active. Once DELIVERED or CANCELLED, rider marker disappears.
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED) {
            com.group130.laundryapp.laundry2_0.Domain.Entity.Rider activeRider = null;
            if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PICKED_UP || order.getStatus() == OrderStatus.IN_PROGRESS) {
                activeRider = order.getPickupRider();
            } else if (order.getStatus() == OrderStatus.READY || order.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                activeRider = order.getDropoffRider();
            }

            if (activeRider != null && activeRider.getCurrentLat() != null && activeRider.getCurrentLng() != null) {
                riderLat = activeRider.getCurrentLat();
                riderLng = activeRider.getCurrentLng();
                isRiderActive = true;
            }
        }

        return orderInfo.builder()
                .OrderId(order.getId())
                .OrderNumber(order.getOrderNumber())
                .UserId(order.getUser() != null ? order.getUser().getId() : null)
                .BusinessId(business != null ? business.getId() : null)
                .BusinessName(business != null ? business.getBusinessName() : null)
                .BusinessAddress(business != null ? business.getAddress() : null)
                .BusinessLat(business != null ? business.getLatitude() : null)
                .BusinessLng(business != null ? business.getLongitude() : null)
                .PickUpRiderId(order.getPickupRider() != null ? order.getPickupRider().getId() : null)
                .DelivaryRiderId(order.getDropoffRider() != null ? order.getDropoffRider().getId() : null)
                .ActiveRiderLat(riderLat)
                .ActiveRiderLng(riderLng)
                .RiderActive(isRiderActive)
                .Status(order.getStatus())
                .PickupAddress(order.getPickupAddress())
                .PickupLat(order.getPickupLat())
                .PickupLng(order.getPickupLng())
                .PickupTime(order.getPickupTime())
                .DeliveryAddress(order.getDeliveryAddress())
                .DeliveryLat(order.getDeliveryLat())
                .DeliveryLng(order.getDeliveryLng())
                .DeliveryTime(order.getDeliveryTime())
                .Subtotal(order.getSubtotal() != null ? order.getSubtotal().doubleValue() : 0.0)
                .PickUpFee(order.getPickupFee() != null ? order.getPickupFee().doubleValue() : 0.0)
                .DropoffFee(order.getDropoffFee() != null ? order.getDropoffFee().doubleValue() : 0.0)
                .DeliveryFee(order.getDeliveryFee() != null ? order.getDeliveryFee().doubleValue() : 0.0)
                .DiscountAmount(0.0)
                .TotalAmount(order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0)
                .Notes(order.getNotes())
                .CancelledReason(order.getCancelledReason())
                .CreatedAt(order.getCreatedAt())
                .UpdatedAt(order.getUpdatedAt())
                .orderItems(itemDTOs)
                .build();
    }

}
