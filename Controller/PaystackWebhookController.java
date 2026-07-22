package com.group130.laundryapp.laundry2_0.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group130.laundryapp.laundry2_0.DAL.Repository.OrderRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.PaymentRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Order;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Payment;
import com.group130.laundryapp.laundry2_0.Domain.Enum.OrderStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * PaystackWebhookController
 *
 * Source of truth for "did this payment actually succeed."
 * Paystack POSTs here for every event; we only act on
 * "charge.success" and verify the signature on every request.
 *
 * Must be registered as the Webhook URL in the Paystack
 * Dashboard, and must be in PUBLIC_ROUTES (Paystack has no JWT).
 */
@RestController
@RequestMapping("/api/v1/webhooks/paystack")
@RequiredArgsConstructor
@Slf4j
public class PaystackWebhookController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;
    private final ObjectMapper      objectMapper;

    @Value("${paystack.secret-key}")
    private String secretKey;

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader("x-paystack-signature") String signature
    ) {
        if (!isValidSignature(rawBody, signature)) {
            log.warn("Paystack webhook signature mismatch — possible spoofed request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            String event = payload.get("event").asText();

            if ("charge.success".equals(event)) {
                handleChargeSuccess(payload.get("data"));
            }

        } catch (Exception e) {
            log.error("Error processing Paystack webhook", e);
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * Reference format: "ORDER_<32-char-uuid>_<SERVICE|RIDE_PICKUP|RIDE_DROPOFF>"
     * Only a successful SERVICE payment advances the order to CONFIRMED —
     * a ride payment succeeding doesn't, since the order can still be
     * unpaid for the laundry itself.
     */
    private void handleChargeSuccess(JsonNode data) {
        String reference = data.get("reference").asText();
        String status     = data.get("status").asText();

        if (!"success".equals(status)) return;

        String withoutPrefix = reference.replace("ORDER_", "");
        int lastUnderscore = withoutPrefix.lastIndexOf('_');
        String orderIdRaw = withoutPrefix.substring(0, lastUnderscore);
        PaymentType paymentType =
                PaymentType.valueOf(withoutPrefix.substring(lastUnderscore + 1));

        UUID orderId = parseUuidFromRaw(orderIdRaw);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found for reference " + reference));

        Payment payment = paymentRepository.findByOrderIdAndPaymentType(orderId, paymentType)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment record not found for order " + orderId + " type " + paymentType));

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionRef(reference);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        if (paymentType == PaymentType.SERVICE
                && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        log.info("{} payment confirmed via webhook for order {}", paymentType, orderId);
    }

    private UUID parseUuidFromRaw(String raw) {
        String withDashes = raw.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
        return UUID.fromString(withDashes);
    }

    private boolean isValidSignature(String rawBody, String receivedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying Paystack signature", e);
            return false;
        }
    }
}