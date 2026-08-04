package com.group130.laundryapp.DAL.Configuration.Auth.AuthSupport;


// ============================================================
//  TokenResponseFilter.java
//  After login/register, automatically writes the new access
//  token into the response Authorization header so the client
//  can read it from either the header or the body.
//
//  This replaces the TokenInterceptor pattern from your
//  uploaded code — that pattern was a client-side HTTP
//  interceptor; this is the server-side equivalent that
//  ensures every auth response carries the token in the header.
// ============================================================

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TokenResponseFilter — intercepts responses from auth endpoints and
 * copies the access token (already in the JSON body) into the
 * Authorization response header automatically.
 *
 * Client receives the token in TWO places:
 *   Header: Authorization: Bearer <token>
 *   Body:   { "access_token": "<token>", ... }
 *
 * This means mobile/web clients can grab it from either location.
 *
 * Order(1) ensures this runs early in the filter chain.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class TokenResponseFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Wrap the response so we can read its body before it's sent
        TokenCapturingResponseWrapper responseWrapper =
                new TokenCapturingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        // Only inject header for auth endpoints
        String path = request.getRequestURI();
        if (path.contains("/api/v1/auth/")) {
            String body = responseWrapper.getCapturedBody();
            String token = extractTokenFromBody(body);
            if (token != null) {
                // Expose header to cross-origin clients (important for CORS)
                response.setHeader("Authorization", "Bearer " + token);
                response.setHeader("Access-Control-Expose-Headers", "Authorization");
            }
        }

        // Write the original body back to the real response
        responseWrapper.copyBodyToResponse();
    }

    /** Parses "access_token" value from a JSON string — no Jackson dependency needed. */
    private String extractTokenFromBody(String body) {
        if (body == null || !body.contains("access_token")) return null;
        try {
            int start = body.indexOf("\"access_token\"");
            if (start == -1) return null;
            int colon  = body.indexOf(":", start);
            int quote1 = body.indexOf("\"", colon);
            int quote2 = body.indexOf("\"", quote1 + 1);
            if (quote1 == -1 || quote2 == -1) return null;
            return body.substring(quote1 + 1, quote2);
        } catch (Exception e) {
            return null;
        }
    }
}