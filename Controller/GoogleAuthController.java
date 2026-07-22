package com.group130.laundryapp.laundry2_0.Controller;


import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * GoogleAuthController
 *
 * This controller does NOT handle the OAuth flow itself —
 * Spring's OAuth2 client owns those URLs internally.
 *
 * What this controller does:
 *   1. GET  /api/v1/auth/google/signin
 *      → Convenience redirect — frontend can call this and Spring
 *        will immediately redirect the browser to Google's login page.
 *        One clean URL for your mobile/web clients to remember.
 *
 *   2. GET  /api/v1/auth/google/signin-url
 *      → Returns the trigger URL as JSON so the frontend can
 *        build the link dynamically (useful for mobile apps).
 *
 *   3. GET  /api/v1/auth/google/callback/status
 *      → Health-check: confirms the callback route is reachable.
 *        Useful during setup to verify your redirect URI config.
 *
 * The actual OAuth flow URLs (managed entirely by Spring):
 *   Trigger  : GET /api/v1/auth/google/login/google
 *   Callback : GET /api/v1/auth/google/callback/google
 */
@RestController
@RequestMapping("/api/v1/auth/google")
@Slf4j
public class GoogleAuthController {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.address:localhost}")
    private String serverAddress;

    // -----------------------------------------------
    //  1. Convenience redirect
    //     Frontend hits this → immediately goes to Google
    //
    //     Mobile:  launchUrl("http://host:8080/api/v1/auth/google/signin")
    //     Web:     window.location.href = "/api/v1/auth/google/signin"
    //     Browser: navigate to http://localhost:8080/api/v1/auth/google/signin
    // -----------------------------------------------
    @GetMapping("/signin")
    public void signIn(HttpServletResponse response) throws IOException {
        /*
         * Redirects to the Spring OAuth2 trigger URL.
         * Spring intercepts /api/v1/auth/google/login/google and
         * starts the Google OAuth flow from there.
         *
         * Why not just use /api/v1/auth/google/login/google directly?
         * You can — but /signin is shorter, cleaner, and easier to
         * remember. This endpoint just bridges to it.
         */
        log.info("Google sign-in initiated — redirecting to OAuth trigger");
        response.sendRedirect("/api/v1/auth/google/login/google");
    }

    // -----------------------------------------------
    //  2. Returns the trigger URL as JSON
    //     Useful for mobile apps that need to build
    //     the URL dynamically or display it in a button.
    //
    //     Response:
    //     {
    //       "signin_url": "http://192.168.x.x:8080/api/v1/auth/google/signin",
    //       "note": "Open this URL in the device browser"
    //     }
    // -----------------------------------------------
    @GetMapping("/signin-url")
    public ResponseEntity<Map<String, String>> getSignInUrl() {
        String baseUrl = "http://" + serverAddress + ":" + serverPort;
        String signinUrl = baseUrl + "/api/v1/auth/google/signin";

        return ResponseEntity.ok(Map.of(
                "signin_url", signinUrl,
                "note", "Open this URL in the device browser to start Google sign-in"
        ));
    }

    // -----------------------------------------------
    //  3. Callback status check
    //     Hit this to confirm your redirect URI setup is correct.
    //     If this returns 200, the route is reachable.
    //     (The real callback /api/v1/auth/google/callback/google
    //      is handled by Spring — this is just a diagnostic.)
    // -----------------------------------------------
    @GetMapping("/callback/status")
    public ResponseEntity<Map<String, String>> callbackStatus() {
        return ResponseEntity.ok(Map.of(
                "status",        "reachable",
                "callback_url",  "/api/v1/auth/google/callback/google",
                "note",          "This route is working. The real callback is handled by Spring OAuth2."
        ));
    }
}