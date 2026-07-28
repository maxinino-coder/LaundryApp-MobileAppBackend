package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Service.RiderService;
import com.group130.laundryapp.laundry2_0.DAL.Service.OrderService;
import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
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
    private final OrderService orderService;

    @GetMapping("/get_rides")
    public ResponseEntity<List<availableOrderInfo>> GetRides(
            @RequestParam(name = "accountId", required = false) UUID accountId
    ){
        return ResponseEntity.ok(riderService.getAvailableOrders(accountId));
    }

    // Get rider profile by account id
    @GetMapping("/{accountId}/profile")
    public ResponseEntity<riderInfo> getRiderInfo(@PathVariable UUID accountId) {
        return ResponseEntity.ok(riderService.getRiderProfile(accountId));
    }

    // Apply for a ride — riderId here is the rider's ACCOUNT id; service resolves it
    @PostMapping("/{riderId}/apply/{orderId}")
    public ResponseEntity<RidesAssignmentResponseDTO> riderApply(
            @PathVariable UUID riderId,
            @PathVariable UUID orderId) {

        RidesAssignmentResponseDTO application = riderService.riderApplyForOrder(riderId, orderId);
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

    /**
     * GET /api/v1/riders/{accountId}/active_jobs
     * Returns orders where this rider is the assigned pickup or dropoff rider
     * and the order is still in-progress (not completed/cancelled).
     * Used by the rider's "Jobs" tab to show their current active assignment.
     */
    @GetMapping("/{accountId}/active_jobs")
    public ResponseEntity<List<orderInfo>> getActiveJobs(@PathVariable UUID accountId) {
        return ResponseEntity.ok(riderService.getActiveJobs(accountId));
    }

    /**
     * GET /api/v1/riders/{accountId}/orders/{orderId}/job_status
     * The rider's route screen polls this to confirm it's still authorized
     * to view this job. A 4xx/500 here means "get out" — reassigned,
     * rejected, or the order was cancelled.
     */
    @GetMapping("/{accountId}/orders/{orderId}/job_status")
    public ResponseEntity<orderInfo> getJobStatus(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(riderService.getJobStatus(accountId, orderId));
    }

    @PostMapping("/{accountId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID accountId,
            @RequestBody UpdateRiderLocationDTO request) {
        riderService.updateRiderLocation(accountId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok().build();
    }
}
