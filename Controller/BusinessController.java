package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Service.BusinessService;
import com.group130.laundryapp.laundry2_0.DAL.Service.OrderService;
import com.group130.laundryapp.laundry2_0.DAL.Service.UserService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
//import com.group130.laundryapp.laundry2_0.Domain.Entity.ServiceItem;
import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.AuthSupport.UserContextHolder;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Business;
import com.group130.laundryapp.laundry2_0.Domain.Entity.BusinessPayout;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Order;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessController {

    // FIX 1: must be private final so @RequiredArgsConstructor injects them
    private final BusinessService     businessService;
    private final OrderService        orderService;
    private final UserContextHolder   userContextHolder;

    @GetMapping("/user_info")
    public ResponseEntity<List<userInfo>> getUserInfo() {
        UUID accountId = userContextHolder.getCurrentAccountId();
        return ResponseEntity.ok(businessService.getUsersByBusinessPurchased(accountId));
    }

    @GetMapping("/business_orders")
    public ResponseEntity<List<orderInfo>> getBusinessOrders(@RequestParam UUID accountId) {
        return ResponseEntity.ok(businessService.getBusinesOrders(accountId));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<orderInfo> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        // DELIVERED is terminal — route through completeOrder so the
        // dropoff rider payout and assignment cleanup also run.
        Order updatedOrder = request.getOrderStatus() == OrderStatus.DELIVERED
                ? orderService.completeOrder(orderId)
                : businessService.updateOrderStatus(orderId, request.getOrderStatus());
        return ResponseEntity.ok(orderInfo.from(updatedOrder, List.of()));
    }
    //Update Business Profile
    @PatchMapping("/{accountId}/update_profile")
    public ResponseEntity<Business> updateBusinessProfile(
            @PathVariable UUID accountId,
            @RequestBody UpdateBusinessProfile request)
    {
        Business updateProfile = businessService.updateBusinessProfile(accountId, request);
        return  ResponseEntity.ok(updateProfile);
    }

    @GetMapping("/{accountId}/business_payout")
    public ResponseEntity<BusinessPayoutDTO> getBusinessPayout(
            @PathVariable UUID accountId
    ){
        return ResponseEntity.ok(businessService.getBusinessPayout(accountId));
    }

    @GetMapping("/{accountId}/business_info")
    public ResponseEntity<Business> getBusinessInfo(@PathVariable UUID accountId){
        return ResponseEntity.ok(businessService.getBusinessByAccountId(accountId));
    }


//    @PostMapping("/create_service_items")
//    public ResponseEntity<ServiceItem> createServiceItems(
//            @RequestBody CreateServiceItemRequest request) {
//        return ResponseEntity.ok(businessService.createServiceItem(request));
//    }



}