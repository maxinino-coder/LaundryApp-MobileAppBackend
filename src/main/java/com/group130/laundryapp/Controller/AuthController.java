package com.group130.laundryapp.Controller;


import com.group130.laundryapp.Domain.DTO.*;
import com.group130.laundryapp.DAL.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Registration endpoints per actor ──────────────────

    @PostMapping("/register/user")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody RegisterUserRequest req) {
        return ResponseEntity.ok(authService.registerUser(req));
    }

    @PostMapping("/register/business")
    public ResponseEntity<AuthResponse> registerBusiness(@RequestBody RegisterBusinessRequest req) {
        return ResponseEntity.ok(authService.registerBusiness(req));
    }

    @PostMapping("/register/rider")
    public ResponseEntity<AuthResponse> registerRider(@RequestBody RegisterRiderRequest req) {
        return ResponseEntity.ok(authService.registerRider(req));
    }

    // ── Universal login (one endpoint for all actors) ─────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        // The role in the response body and Authorization header tells the
        // client which actor they are — no separate /login/user, /login/rider needed.
        return ResponseEntity.ok(authService.login(req));
    }

    // ── Token refresh ──────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        // Refresh token sent in X-Refresh-Token header (not Authorization)
        // to avoid confusion with the access token.
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }
}
