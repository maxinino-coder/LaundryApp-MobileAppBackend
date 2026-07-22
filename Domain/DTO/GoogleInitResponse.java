package com.group130.laundryapp.laundry2_0.Domain.DTO;

// -----------------------------------------------
//  Response types
// -----------------------------------------------

/**
 * Returned by initBusinessOrRider().
 * isExistingAccount = true  → authResponse is populated, send to frontend
 * isExistingAccount = false → googlePayload is populated, frontend collects extra fields
 */
public record GoogleInitResponse(
        boolean            isExistingAccount,
        AuthResponse       authResponse,       // non-null if existing
        String             partialToken,       // reserved for future use
        GoogleOAuthPayload googlePayload       // non-null if new (pre-fills the form)
) {}