package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBusinessProfile {
    // The app sends camelCase JSON — @JsonAlias lets both spellings bind
    @JsonAlias("businessName")   private String business_name;
    private String description;
    @JsonAlias("logoUrl")        private String logo_url;
    @JsonAlias("bannerUrl")      private String banner_url;
    private String address;
    private String city;
    private String latitude;
    private String longitude;
//    private Boolean is_open;
    @JsonAlias("openingTime")    private LocalTime opening_time;
    @JsonAlias("closingTime")    private LocalTime closing_time;
    @JsonAlias("bankName")       private String bank_name;
    @JsonAlias("bankAccountNo")  private String bank_account_no;
    @JsonAlias("bankAccountName") private String bank_account_name;
    @JsonAlias("momoNumber")     private String momo_number;
    @JsonAlias("createdAt")      private OffsetDateTime created_at;
    @JsonAlias("updatedAt")      private OffsetDateTime updated_at;
    @JsonAlias("paystackSubaccountCode") private String paystack_subaccount_code;
}
