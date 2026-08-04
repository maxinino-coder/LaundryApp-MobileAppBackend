package com.group130.laundryapp.Domain.DTO;
import com.group130.laundryapp.Domain.Enum.RiderType;
import com.group130.laundryapp.Domain.Enum.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRiderRequest
         {
             String email;
             String phone;
             String password;
             String firstName;
             String lastName;
             VehicleType vehicleType;
             RiderType riderType;
             java.util.UUID businessId;
}  // null for CONTRACT riders
