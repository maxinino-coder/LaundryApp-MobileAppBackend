package com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth;


import com.group130.laundryapp.laundry2_0.Domain.Entity.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
@Service
public class JwtConfig {

    // Injected from application.yml — never hardcode secrets
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;       // default: 900_000  (15 min)

    @Value("${jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;      // default: 604_800_000 (7 days)

    // -----------------------------------------------
    //  Token generation
    // -----------------------------------------------

    /**
     * Generate a short-lived ACCESS token.
     * Extra claims embedded: role, accountId.
     * These are read by JwtAuthenticationFilter so the SecurityContext
     * is fully populated without any extra DB call.
     */
    public String generateAccessToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",      account.getRole().name());      // USER | BUSINESS | RIDER
        claims.put("accountId", account.getId().toString());
        claims.put("type",      "ACCESS");
        return buildToken(claims, account.getEmail(), accessTokenExpiryMs);
    }

    /**
     * Generate a long-lived REFRESH token.
     * Carries minimal claims — just enough to identify the account and
     * verify it hasn't been rotated/revoked.
     */
    public String generateRefreshToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accountId", account.getId().toString());
        claims.put("type",      "REFRESH");
        claims.put("jti",       UUID.randomUUID().toString()); // unique per token
        return buildToken(claims, account.getEmail(), refreshTokenExpiryMs);
    }

    // -----------------------------------------------
    //  Claim extraction
    // -----------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractAccountId(String token) {
        return extractClaim(token, claims -> claims.get("accountId", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // -----------------------------------------------
    //  Validation
    // -----------------------------------------------

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        final String type     = extractTokenType(token);
        return username.equals(userDetails.getUsername())
                && "ACCESS".equals(type)
                && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(String token) {
        try {
            return "REFRESH".equals(extractTokenType(token)) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // -----------------------------------------------
    //  Internal helpers
    // -----------------------------------------------

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiryMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private java.security.Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
