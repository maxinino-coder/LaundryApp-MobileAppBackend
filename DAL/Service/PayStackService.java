package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackRequestDTO.InitializeTransactionRequest;
import com.group130.laundryapp.laundry2_0.Domain.DTO.PayStackResponseDTO.PaystackResponse;
import com.group130.laundryapp.laundry2_0.Domain.Enum.PaymentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * PayStackService
 *
 * The class that talks to Paystack's API directly.
 * Four operations:
 *   1. initializeTransaction — start any of the three payment types
 *   2. createSubaccount      — one-time business onboarding
 *   3. createTransferRecipient — one-time rider onboarding
 *   4. initiateTransfer      — pay out a rider after leg confirmation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayStackService {

    private final WebClient paystackWebClient;

    // -----------------------------------------------
    //  1. INITIALIZE TRANSACTION
    // -----------------------------------------------

    public String initializeTransaction(
            String customerEmail,
            BigDecimal amountInMainUnit,
            String subaccountCode,     // nullable
            UUID orderId,
            PaymentType paymentType
    ) {
        long amountInSubunit = toSubunit(amountInMainUnit);
        String reference = "ORDER_" + orderId.toString().replace("-", "")
                + "_" + paymentType.name();

        InitializeTransactionRequest.InitializeTransactionRequestBuilder builder =
                InitializeTransactionRequest.builder()
                        .email(customerEmail)
                        .amount(amountInSubunit)
                        .reference(reference)
                        .currency("GHS")
                        .metadata(Map.of(
                                "order_id", orderId.toString(),
                                "payment_type", paymentType.name()
                        ));

        if (subaccountCode != null) {
            builder.subaccount(subaccountCode);
        }

        InitializeTransactionRequest request = builder.build();

        PaystackResponse<Map> response;
        try {
            response = paystackWebClient.post()
                    .uri("/transaction/initialize")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaystackResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Paystack initialize transaction error for order {}: {}", orderId, e.getMessage());
            throw new IllegalStateException("Payment service is currently unavailable. Please check your internet connection.");
        }

        if (response == null || !response.isStatus()) {
            throw new IllegalStateException(
                    "Paystack failed to initialize " + paymentType + " transaction for order " + orderId);
        }

        Map<String, Object> data = (Map<String, Object>) response.getData();
        return (String) data.get("authorization_url");
    }

    // -----------------------------------------------
    //  2. CREATE SUBACCOUNT  (business onboarding, one-time)
    // -----------------------------------------------

    public String createSubaccount(
            String businessName,
            String bankCode,
            String accountNumber,
            double percentageChargeToBusiness
    ) {
        Map<String, Object> request = Map.of(
                "business_name", businessName,
                "settlement_bank", bankCode,
                "account_number", accountNumber,
                "percentage_charge", percentageChargeToBusiness
        );

        PaystackResponse<Map> response;
        try {
            response = paystackWebClient.post()
                    .uri("/subaccount")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaystackResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Paystack create subaccount error for business {}: {}", businessName, e.getMessage());
            throw new IllegalStateException("Failed to reach payment gateway to create business subaccount.");
        }

        if (response == null || !response.isStatus()) {
            throw new IllegalStateException("Failed to create Paystack subaccount for " + businessName);
        }

        Map<String, Object> data = (Map<String, Object>) response.getData();
        return (String) data.get("subaccount_code");
    }

    // -----------------------------------------------
    //  3. CREATE TRANSFER RECIPIENT  (rider onboarding, one-time)
    // -----------------------------------------------

    public String createTransferRecipient(
            String riderName,
            String momoNumber,
            String momoProviderCode   // "MTN", "VOD", "ATL"
    ) {
        Map<String, Object> request = Map.of(
                "type", "mobile_money",
                "name", riderName,
                "account_number", momoNumber,
                "bank_code", momoProviderCode,
                "currency", "GHS"
        );

        PaystackResponse<Map> response;
        try {
            response = paystackWebClient.post()
                    .uri("/transferrecipient")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaystackResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Paystack create transfer recipient error for rider {}: {}", riderName, e.getMessage());
            throw new IllegalStateException("Failed to reach payment gateway to set up rider payout account.");
        }

        if (response == null || !response.isStatus()) {
            throw new IllegalStateException("Failed to create transfer recipient for rider " + riderName);
        }

        Map<String, Object> data = (Map<String, Object>) response.getData();
        return (String) data.get("recipient_code");
    }

    // -----------------------------------------------
    //  4. INITIATE TRANSFER  (pay a rider, triggered on leg confirmation)
    // -----------------------------------------------

    public String initiateTransfer(
            String recipientCode,
            BigDecimal amountInMainUnit,
            UUID riderEarningId
    ) {
        long amountInSubunit = toSubunit(amountInMainUnit);
        String reference = "PAYOUT_" + riderEarningId.toString().replace("-", "");

        Map<String, Object> request = Map.of(
                "source", "balance",
                "amount", amountInSubunit,
                "recipient", recipientCode,
                "reason", "Delivery fee payout",
                "reference", reference
        );

        PaystackResponse<Map> response;
        try {
            response = paystackWebClient.post()
                    .uri("/transfer")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaystackResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Paystack initiate transfer error for rider earning {}: {}", riderEarningId, e.getMessage());
            throw new IllegalStateException("Failed to reach payment gateway for rider payout.");
        }

        if (response == null || !response.isStatus()) {
            throw new IllegalStateException("Failed to initiate transfer for rider earning " + riderEarningId);
        }

        Map<String, Object> data = (Map<String, Object>) response.getData();
        return (String) data.get("reference");
    }

    // -----------------------------------------------
    //  Helpers
    // -----------------------------------------------

    private long toSubunit(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}