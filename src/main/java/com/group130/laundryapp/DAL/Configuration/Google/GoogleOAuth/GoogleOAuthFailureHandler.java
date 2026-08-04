package com.group130.laundryapp.DAL.Configuration.Google.GoogleOAuth;

// ============================================================
//  GoogleOAuthFailureHandler.java
//  Called when anything goes wrong in the OAuth flow:
//  - User cancelled the Google login popup
//  - State mismatch (CSRF attempt)
//  - Google returned an error
//  Redirects browser to the frontend with an error reason.
// ============================================================

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class GoogleOAuthFailureHandler implements AuthenticationFailureHandler {

    @Value("${google.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest  request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        log.warn("Google OAuth failure: {}", exception.getMessage());

        String reason = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        response.sendRedirect(frontendRedirectUrl + "/auth/error?reason=" + reason);
    }
}


// ============================================================
//  HOW THE GOOGLE BUTTON WORKS IN YOUR FRONTEND
//
//  The frontend does NOT call any Google SDK.
//  The button is just a plain link or redirect to your backend URL.
//
//  React example:
//  ─────────────
//  <a href="http://localhost:8080/api/v1/auth/google/user">
//    <button>Sign in with Google</button>
//  </a>
//
//  Or programmatically:
//  window.location.href = "http://localhost:8080/api/v1/auth/google/user";
//
//  Flutter / mobile example:
//  ─────────────────────────
//  Use url_launcher to open the backend URL in the device browser:
//
//  await launchUrl(
//    Uri.parse("http://localhost:8080/api/v1/auth/google/user"),
//    mode: LaunchMode.externalApplication,
//  );
//
//  The browser handles all the redirects. After Google redirects back to
//  your backend callback and your backend redirects to:
//    http://localhost:3000/auth/callback?access_token=xxx&refresh_token=yyy&role=USER
//
//  Your frontend reads the params and stores the tokens:
//  ─────────────────────────────────────────────────────
//  // React (in the /auth/callback page component):
//  const params = new URLSearchParams(window.location.search);
//  const accessToken  = params.get("access_token");
//  const refreshToken = params.get("refresh_token");
//  const role         = params.get("role");
//
//  localStorage.setItem("access_token",  accessToken);
//  localStorage.setItem("refresh_token", refreshToken);
//  localStorage.setItem("role",          role);
//
//  // Clean the tokens out of the URL (don't leave them visible)
//  window.history.replaceState({}, "", "/dashboard");
//
//  // Now use the access token in all subsequent API calls:
//  fetch("/api/v1/orders/user/...", {
//    headers: { "Authorization": "Bearer " + accessToken }
//  });
//
// ============================================================


// ============================================================
//  WHAT SPRING DOES AUTOMATICALLY (you write NO controller for this)
//
//  When the browser hits GET /api/v1/auth/google/user:
//    Spring generates a random `state` value (CSRF protection)
//    Spring stores it in the session
//    Spring builds the full Google auth URL:
//      https://accounts.google.com/o/oauth2/v2/auth
//        ?client_id=YOUR_CLIENT_ID
//        &redirect_uri=http://localhost:8080/api/v1/auth/google/callback
//        &response_type=code
//        &scope=openid+email+profile
//        &state=RANDOM_STATE
//    Spring sends a 302 redirect to that URL
//    Browser follows it — user sees Google's login page
//
//  When Google redirects back to /api/v1/auth/google/callback?code=XXX&state=YYY:
//    Spring validates state matches what was stored (CSRF check)
//    Spring POSTs to https://oauth2.googleapis.com/token with the code
//    Google returns { access_token, id_token, expires_in, ... }
//    Spring fetches profile from https://www.googleapis.com/oauth2/v3/userinfo
//    Spring calls YOUR GoogleOAuthSuccessHandler with the full user profile
//
//  You write ZERO code for any of that. Spring OAuth2 client handles it all.
//  Your only job is the SuccessHandler (what to do with the verified user).
// ============================================================


// ============================================================
//  ENVIRONMENT VARIABLES SUMMARY
//  Add all of these to IntelliJ run config or your .env file:
//
//  GOOGLE_CLIENT_ID      = 123456789-abc.apps.googleusercontent.com
//  GOOGLE_CLIENT_SECRET  = GOCSPX-xxxxxxxxxxxxxxxxxxxxxx
//  FRONTEND_URL          = http://localhost:3000
//  JWT_SECRET            = (your existing secret)
//  DB_USERNAME           = postgres
//  DB_PASSWORD           = (your db password)
//
//  In Google Console:
//    Authorised JavaScript origins → LEAVE EMPTY
//    Authorised redirect URIs      → http://localhost:8080/api/v1/auth/google/callback
// ============================================================
