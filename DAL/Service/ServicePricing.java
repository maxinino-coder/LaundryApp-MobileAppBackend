package com.group130.laundryapp.laundry2_0.DAL.Service;

import com.group130.laundryapp.laundry2_0.Domain.Enum.ServiceCategory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Category-based price table — the single source of truth for order pricing.
 * The ServiceItem catalog entity was intentionally removed to keep things
 * simple; the frontend mirrors these numbers in its item picker
 * (laundryGo/constants/services.ts), so change both together.
 * Prices are GHS per item.
 */
public final class ServicePricing {

    public static final Map<ServiceCategory, BigDecimal> UNIT_PRICES = Map.of(
            ServiceCategory.WASH,           new BigDecimal("10.00"),
            ServiceCategory.WASH_FOLD,      new BigDecimal("15.00"),
            ServiceCategory.WASH_FOLD_IRON, new BigDecimal("20.00"),
            ServiceCategory.OTHER,          new BigDecimal("12.00")
    );

    /** Flat per-leg rider fees (GHS). */
    public static final BigDecimal PICKUP_FEE  = new BigDecimal("8.00");
    public static final BigDecimal DROPOFF_FEE = new BigDecimal("8.00");

    private ServicePricing() {}

    public static BigDecimal priceFor(ServiceCategory category) {
        return UNIT_PRICES.getOrDefault(category, UNIT_PRICES.get(ServiceCategory.OTHER));
    }
}
