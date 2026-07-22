package com.group130.laundryapp.laundry2_0.DAL.Service;


// ============================================================
//  AuthService.java
//  Handles register + login for USER, BUSINESS, RIDER.
//  Returns AuthResponse with both tokens + role.
// ============================================================

import com.group130.laundryapp.laundry2_0.Domain.DTO.*;
import com.group130.laundryapp.laundry2_0.Domain.Entity.*;
import com.group130.laundryapp.laundry2_0.Domain.Enum.AccountRole;
import com.group130.laundryapp.laundry2_0.DAL.Repository.*;
import com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth.JwtConfig;
import com.group130.laundryapp.laundry2_0.Domain.Enum.RiderType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final RiderRepository riderRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    // -----------------------------------------------
    //  REGISTER
    // -----------------------------------------------

    /**
     * Register a new customer (USER)
     */
    @Transactional
    public AuthResponse registerUser(RegisterUserRequest req) {
        Account account = createAccount(req.getEmail(), req.getPhone(), req.getPassword(), AccountRole.USER);

        User user = User.builder()
                .account(account)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .build();
        userRepository.save(user);

        return buildAuthResponse(account);
    }

    /**
     * Register a new laundry business (BUSINESS)
     */
    @Transactional
    public AuthResponse registerBusiness(RegisterBusinessRequest req) {
        Account account = createAccount(req.getEmail(), req.getPhone(), req.getPassword(), AccountRole.BUSINESS);

        Business business = Business.builder()
                .account(account)
                .businessName(req.getBusinessName())
                .address(req.getAddress())
                .city(req.getCity())
                .build();
        businessRepository.save(business);

        return buildAuthResponse(account);
    }

    /**
     * Register a new rider (RIDER) — requires a business to employ them, or contract
     */
    @Transactional
    public AuthResponse registerRider(RegisterRiderRequest req) {
        Account account = createAccount(req.getEmail(), req.getPhone(), req.getPassword(), AccountRole.RIDER);

        Rider.RiderBuilder builder = Rider.builder()
                .account(account)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .vehicleType(req.getVehicleType())
                .riderType(req.getRiderType());

        // EMPLOYED riders must link to a business
        if (req.getRiderType() == RiderType.EMPLOYED) {
            Business business = businessRepository.findById(req.getBusinessId())
                    .orElseThrow(() -> new IllegalArgumentException("Business not found"));
            builder.business(business);
        }

        riderRepository.save(builder.build());
        return buildAuthResponse(account);
    }

    // -----------------------------------------------
    //  LOGIN  (same endpoint for all actors)
    // -----------------------------------------------

    /**
     * Universal login — works for USER, BUSINESS, RIDER.
     * Spring Security finds the Account by email and validates the password.
     * The role in the returned token tells the client who they are.
     */
    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException if wrong password
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        Account account = accountRepository.findByEmail(req.email())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        // Update last login timestamp
        account.setLastLoginAt(OffsetDateTime.now());
        accountRepository.save(account);

        return buildAuthResponse(account);
    }

    // -----------------------------------------------
    //  REFRESH TOKEN
    // -----------------------------------------------

    /**
     * Exchange a valid refresh token for a new access + refresh token pair.
     * Old refresh token is revoked (rotation pattern — prevents reuse).
     */
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtConfig.isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is expired or invalid.");
        }

        String email = jwtConfig.extractUsername(refreshToken);
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

        // Revoke the used refresh token (token rotation)
        String incomingHash = hashToken(refreshToken);
        refreshTokenRepository.findByRefreshTokenHash(incomingHash)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });

        return buildAuthResponse(account);
    }

    // -----------------------------------------------
    //  Internal helpers
    // -----------------------------------------------

    private Account createAccount(String email, String phone, String rawPassword, AccountRole role) {
        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered.");
        }
        if (accountRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Phone number already registered.");
        }
        Account account = Account.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        return accountRepository.save(account);
    }

    private AuthResponse buildAuthResponse(Account account) {
        String accessToken = jwtConfig.generateAccessToken(account);
        String refreshToken = jwtConfig.generateRefreshToken(account);

        // Persist the refresh token (hashed) so we can revoke it later
        saveRefreshToken(account, refreshToken, accessToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(account.getRole().name())
                .accountId(account.getId().toString())
                .build();
    }

    private void saveRefreshToken(Account account, String rawRefreshToken, String rawAccessToken) {
        RefreshToken token = RefreshToken.builder()
                .account(account)
                .refreshTokenHash(hashToken(rawRefreshToken))
                .accessTokenHash(hashToken(rawAccessToken))
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(token);
    }

    /**
     * Simple SHA-256 hex hash — avoids storing raw tokens in DB
     */
    private String hashToken(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
