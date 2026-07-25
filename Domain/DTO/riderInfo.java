package com.group130.laundryapp.laundry2_0.Domain.DTO;

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
public class riderInfo {
    private UUID Id;
    private String FirstName;
    private String LastName;
    private String AvatarUrl;
    private VehicleType VehicleType;
    private RiderType RiderType;
    private String VehiclePlate;
    private Boolean IsAvailable;
    private Boolean IsApproved;
    private Double CurrentLat;
    private Double CurrentLng;
    private Double LastLocationAt;
    private String MomoNumber;
    private String BankAccountNo;
    private OffsetDateTime CreatedAt;
    private OffsetDateTime UpdatedAt;

    }
