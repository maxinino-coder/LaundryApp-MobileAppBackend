package com.group130.laundryapp.laundry2_0.Domain.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.group130.laundryapp.laundry2_0.Domain.Enum.RiderType;
import com.group130.laundryapp.laundry2_0.Domain.Enum.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRiderProfile {

// The app sends camelCase JSON — @JsonAlias lets both spellings bind
@JsonAlias("businessId")    private UUID business_id;
@JsonAlias("firstName")     private String first_name;
@JsonAlias("lastName")      private String last_name;
@JsonAlias("avatarUrl")     private String  avatar_url;
@JsonAlias("vehicleType")   private VehicleType vehicle_type;
@JsonAlias("riderType")     private RiderType rider_type;
@JsonAlias("vehiclePlate")  private String  vehicle_plate;
@JsonAlias("isAvailable")   private Boolean  is_available;
@JsonAlias("isApproved")    private Boolean    is_approved;
@JsonAlias("currentLat")    private String    current_lat;
@JsonAlias("currentLng")    private String current_lng;
@JsonAlias("lastLocationAt") private String last_location_at;
@JsonAlias("momoNumber")    private String  momo_number;
@JsonAlias("bankAccountNo") private String bank_account_no;
@JsonAlias("createdAt")     private OffsetDateTime created_at;
@JsonAlias("updatedAt")     private OffsetDateTime   updated_at;
}
