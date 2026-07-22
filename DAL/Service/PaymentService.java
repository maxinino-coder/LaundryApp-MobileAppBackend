package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.OrderRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.PaymentRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Order;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Payment;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentMethod;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PaymentService
 *
 * Three independent entry points — call any of them in any order,
 * at any time, with no requirement that the others be called first:
 *
 *   payForPickupRide(orderId)  — trip bringing dirty clothes TO business
 *   payForService(orderId)     — the laundry itself, splits to business
 *   payForDropoffRide(orderId) — trip bringing clean clothes BACK to user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository   orderRepository;
    private final PaymentRepository paymentRepository;
    private final PayStackService   paystackService;

    @Transactional
    public String payForPickupRide(UUID orderId, Account payerAccount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return payForRideLeg(order, payerAccount, PaymentType.RIDE_PICKUP, order.getPickupFee());
    }

    @Transactional
    public String payForDropoffRide(UUID orderId, Account payerAccount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return payForRideLeg(order, payerAccount, PaymentType.RIDE_DROPOFF, order.getDropoffFee());
    }

    @Transactional
    public String payForService(UUID orderId, Account payerAccount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getBusiness().getPaystackSubaccountCode() == null) {
            throw new IllegalStateException(
                    "Business has not completed payment setup — cannot accept payment.");
        }

        rejectIfAlreadyPaid(orderId, PaymentType.SERVICE);

        BigDecimal serviceAmount = order.getSubtotal();

        Payment payment = Payment.builder()
                .order(order)
                .payerAccount(payerAccount)
                .paymentType(PaymentType.SERVICE)
                .amount(serviceAmount)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        String authorizationUrl = paystackService.initializeTransaction(
                payerAccount.getEmail(),
                serviceAmount,
                order.getBusiness().getPaystackSubaccountCode(),
                orderId,
                PaymentType.SERVICE
        );

        log.info("Service payment initialized for order {}", orderId);
        return authorizationUrl;
    }

    // -----------------------------------------------
    //  Shared logic for both ride legs
    // -----------------------------------------------

    private String payForRideLeg(
            Order order,
            Account payerAccount,
            PaymentType legType,
            BigDecimal legAmount
    ) {
        rejectIfAlreadyPaid(order.getId(), legType);

        if (legAmount == null || legAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No fee set for " + legType + " on this order.");
        }

        Payment payment = Payment.builder()
                .order(order)
                .payerAccount(payerAccount)
                .paymentType(legType)
                .amount(legAmount)
                .paymentMethod(PaymentMethod.MOBILE_MONEY)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // null subaccount — stays on platform balance until leg confirmed
        String authorizationUrl = paystackService.initializeTransaction(
                payerAccount.getEmail(),
                legAmount,
                null,
                order.getId(),
                legType
        );

        log.info("{} payment initialized for order {}", legType, order.getId());
        return authorizationUrl;
    }

    private void rejectIfAlreadyPaid(UUID orderId, PaymentType type) {
        paymentRepository.findByOrderIdAndPaymentType(orderId, type)
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .ifPresent(p -> {
                    throw new IllegalStateException(type + " for this order has already been paid.");
                });
    }
}