package com.group130.laundryapp.laundry2_0.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class businessInfo {
    private UUID Id;
    private String BusinessName;
    private String Description;
    private String  LogoUrl;
    private String  BannerUrl;
    private String  Address;
    private String   City;
   private Double Latitude;
    private Double  Longitude;
    private Boolean IsOpen;
   private LocalTime OpeningTime;
    private LocalTime ClosingTime;
    private String  BankName;
    private String   BankAccountNo;
    private String   BankAccountName;
    private String   MomoNumber;
    private com.group130.laundryapp.laundry2_0.Domain.Enum.PricingModel PricingModel;
    private java.math.BigDecimal PricePerKg;
}
