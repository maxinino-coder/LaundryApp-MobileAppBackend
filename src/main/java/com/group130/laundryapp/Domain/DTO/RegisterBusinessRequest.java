package com.group130.laundryapp.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterBusinessRequest{
    String email;
    String phone;
    String password;
    String businessName;
    String address;
    String city;
}
