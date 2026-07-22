package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.DAL.Repository.PaymentRepository;
import com.group130.laundryapp.laundry2_0.DAL.Repository.RiderEarningRepository;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Order;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Payment;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentStatus;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import com.group130.laundryapp.laundry2_0.Domain.Entity.Rider;
import com.group130.laundryapp.laundry2_0.Domain.Entity.RiderEarning;
import com.group130.laundryapp.laundry2_0.Domain.Entity.RiderEarning.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * RiderPayoutService
 *
 * Call confirmPickup(order) when the business marks pickup done,
 * and confirmDropoff(order) when dropoff is done. These are two
 * separate triggers, potentially paying two different riders.
 *
 * Each payout verifies the corresponding ride payment actually
 * succeeded before transferring anything — a rider assigned to
 * a leg whose payment failed will not be paid.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiderPayoutService {

    private final RiderEarningRepository riderEarningRepository;
    private final PaymentRepository      paymentRepository;
    private final PayStackService        paystackService;

    @Transactional
    public void confirmPickup(Order order) {
        payoutLeg(order, order.getPickupRider(), PaymentType.RIDE_PICKUP);
    }

    @Transactional
    public void confirmDropoff(Order order) {
        payoutLeg(order, order.getDropoffRider(), PaymentType.RIDE_DROPOFF);
    }

    private void payoutLeg(Order order, Rider rider, PaymentType legType) {
        if (rider == null) {
            log.info("Order {} {} confirmed with no rider assigned — nothing to pay out",
                    order.getId(), legType);
            return;
        }

        Payment legPayment = paymentRepository
                .findByOrderIdAndPaymentType(order.getId(), legType)
                .orElse(null);

        if (legPayment == null || legPayment.getStatus() != PaymentStatus.SUCCESS) {
            log.error("Order {} has a rider assigned for {} but no successful payment — " +
                    "refusing to pay out rider {}", order.getId(), legType, rider.getId());
            return;
        }

        if (rider.getPaystackRecipientCode() == null) {
            log.error("Rider {} has no Paystack recipient code — cannot pay out.", rider.getId());
            return;
        }

        RiderEarning earning = riderEarningRepository
                .findByOrderIdAndLeg(order.getId(), legType)
                .orElseThrow(() -> new IllegalStateException(
                        "No RiderEarning found for order " + order.getId() + " leg " + legType));

        if (earning.getStatus() == SettlementStatus.SETTLED) {
            log.warn("RiderEarning {} already settled — refusing duplicate payout", earning.getId());
            return;
        }

        try {
            String transferReference = paystackService.initiateTransfer(
                    rider.getPaystackRecipientCode(),
                    legPayment.getAmount(),
                    earning.getId()
            );

            earning.setPaystackTransferCode(transferReference);
            earning.setStatus(SettlementStatus.SETTLED);
            earning.setSettledAt(OffsetDateTime.now());
            riderEarningRepository.save(earning);

            log.info("Rider {} paid out {} for order {} leg {} — transfer ref {}",
                    rider.getId(), legPayment.getAmount(), order.getId(), legType, transferReference);

        } catch (Exception e) {
            earning.setStatus(SettlementStatus.FAILED);
            riderEarningRepository.save(earning);
            log.error("Failed to pay out rider {} for order {} leg {}",
                    rider.getId(), order.getId(), legType, e);
        }
    }
}