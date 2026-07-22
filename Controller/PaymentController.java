package com.group130.laundryapp.laundry2_0.Controller;

import com.group130.laundryapp.laundry2_0.DAL.Service.PaymentService;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.AuthSupport.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * PaymentController
 *
 * POST /api/v1/orders/{orderId}/pay/pickup
 * POST /api/v1/orders/{orderId}/pay/service
 * POST /api/v1/orders/{orderId}/pay/dropoff
 *
 * Protected — requires Bearer token with ROLE_USER.
 * Each returns a Paystack authorization_url to redirect the
 * customer to for completing payment.
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService    paymentService;
    private final UserContextHolder userContextHolder;

    @PostMapping("/pickup")
    public ResponseEntity<Map<String, String>> payForPickup(@PathVariable UUID orderId) {
        Account payer = userContextHolder.getCurrentAccount();
        String url = paymentService.payForPickupRide(orderId, payer);
        return ResponseEntity.ok(Map.of("authorization_url", url));
    }

    @PostMapping("/service")
    public ResponseEntity<Map<String, String>> payForService(@PathVariable UUID orderId) {
        Account payer = userContextHolder.getCurrentAccount();
        String url = paymentService.payForService(orderId, payer);
        return ResponseEntity.ok(Map.of("authorization_url", url));
    }

    @PostMapping("/dropoff")
    public ResponseEntity<Map<String, String>> payForDropoff(@PathVariable UUID orderId) {
        Account payer = userContextHolder.getCurrentAccount();
        String url = paymentService.payForDropoffRide(orderId, payer);
        return ResponseEntity.ok(Map.of("authorization_url", url));
    }
}