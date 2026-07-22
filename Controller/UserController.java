package com.group130.laundryapp.laundry2_0.Controller;


import com.group130.laundryapp.laundry2_0.DAL.Service.BusinessService;
import com.group130.laundryapp.laundry2_0.DAL.Service.OrderService;
import com.group130.laundryapp.laundry2_0.DAL.Service.RiderService;
import com.group130.laundryapp.laundry2_0.DAL.Service.UserService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userSercvie;
    private final BusinessService businessService;
    private final RiderService riderService;
    private final OrderService orderService;

    //GetBusinessInfo   //GetBusinessInfo By Name,Location
    @GetMapping("/getbusinessInfo")
    public ResponseEntity<List<businessInfo>> GetBusinessInfo(
            @RequestParam(required = false) String Name,
            @RequestParam(required = false) String Location) {
        return ResponseEntity.ok(businessService.getBusinessInfo(Name, Location));
    }

    //GetUserInfoBy accountId
    @GetMapping("/getuserinfoById")
    public ResponseEntity<userInfo> GetUserInfo(@RequestParam UUID accountId){
        return ResponseEntity.ok(userSercvie.getUserInfo(accountId));
    }

    //GetRiderInfo
    @GetMapping("/getriderinfo")
    public ResponseEntity<List<riderInfo>> GetRiderInfo(){
        return  ResponseEntity.ok(riderService.getRiderInfo());
    }

    //User Makes Order
    @PostMapping("/make_order")
    public ResponseEntity<orderInfo> makeBusinessOrder(@RequestBody CreateBusinessOrderRequest request) {
        orderInfo completeOrder = userSercvie.createBusinessOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(completeOrder);
    }

    // View riders who applied for an order (accountId = the user's auth account id)
    @GetMapping("/{accountId}/orders/{orderId}/applicants")
    public ResponseEntity<List<RideApplicantDTO>> viewApplicants(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderApplicants(orderId, accountId));
    }

    // Accept one rider's application for an order
    @PostMapping("/{accountId}/orders/{orderId}/applicants/{applicationId}/accept")
    public ResponseEntity<orderInfo> acceptApplicant(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(orderService.userAcceptRider(orderId, accountId, applicationId));
    }

    @GetMapping("/customer_orders")
    public ResponseEntity<List<orderInfo>> getCustomerOrders(@RequestParam UUID accountId) {
        return ResponseEntity.ok(orderService.getOrdersById(accountId));
    }

    @PatchMapping("/{accountId}/update_profile")
    public ResponseEntity<userInfo> updateUserProfile(
            @PathVariable UUID accountId,
            @RequestBody UpdateUserProfile request)
    {
        userSercvie.updateUserProfile(accountId, request);
        return ResponseEntity.ok(userSercvie.getUserInfo(accountId));
    }
}
