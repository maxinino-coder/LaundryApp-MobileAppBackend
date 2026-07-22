package com.group130.laundryapp.laundry2_0.Domain.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class userInfo {
    private UUID id;
    private String firstName;
    private String lastName;
    private String Address;
    private  String City;
    private Double latitude;
    private Double longitude;
    private String email;
    private String phone;
}
