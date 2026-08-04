package com.group130.laundryapp.Domain.DTO;


public record GoogleOAuthPayload(
        String googleId,    // Google's stable unique user ID ("sub" claim)
        String email,       // verified email from Google
        String firstName,   // "given_name"
        String lastName,    // "family_name"
        String pictureUrl,  // profile photo URL
        boolean emailVerified
) {}