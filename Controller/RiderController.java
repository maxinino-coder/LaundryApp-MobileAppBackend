package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Service.RiderService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Business;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Rider;
import com.group130.laundryapp.laundry2_0.Domain.Entity.RidesAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/riders")
@RequiredArgsConstructor
public class RiderController {
    private final RiderService riderService;

    @GetMapping("/get_rides")
    public ResponseEntity<List<availableOrderInfo>>  GetRides(){
        return ResponseEntity.ok(riderService.getAvailableOrders());
    }

    // Get rider profile by account id
    @GetMapping("/{accountId}/profile")
    public ResponseEntity<riderInfo> getRiderInfo(@PathVariable UUID accountId) {
        return ResponseEntity.ok(riderService.getRiderProfile(accountId));
    }

    // Apply for a ride — riderId here is the rider's ACCOUNT id; service resolves it
    @PostMapping("/{riderId}/apply/{orderId}")
    public ResponseEntity<RidesAssignment> riderApply(
            @PathVariable UUID riderId,
            @PathVariable UUID orderId) {

        RidesAssignment application = riderService.riderApplyForOrder(riderId, orderId);
        return new ResponseEntity<>(application, HttpStatus.CREATED);
    }

    // Rider confirms they collected the goods from the customer
    @PostMapping("/orders/{orderId}/confirm-pickup")
    public ResponseEntity<Void> confirmPickup(@PathVariable UUID orderId) {
        riderService.confirmPickup(orderId);
        return ResponseEntity.noContent().build();
    }

    // Rider confirms they delivered the goods to the business
    @PostMapping("/orders/{orderId}/confirm-delivery")
    public ResponseEntity<Void> confirmDelivery(@PathVariable UUID orderId) {
        riderService.confirmDelivery(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}/rider_payout")
    public ResponseEntity<RiderPayoutDTO> getRiderPayout(
            @PathVariable UUID accountId
    ){
        return ResponseEntity.ok(riderService.getRiderPayout(accountId));
    }

    @PatchMapping("/{accountId}/update_profile")
    public ResponseEntity<Rider> updateRiderProfile(
            @PathVariable UUID accountId,
            @RequestBody UpdateRiderProfile request)
    {
        Rider updatedProfile = riderService.updateRiderProfile(accountId, request);
        return ResponseEntity.ok(updatedProfile);
    }


}
