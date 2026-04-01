package com.academic.platform.security;

import com.academic.platform.service.UserService;
import com.academic.platform.service.SystemSettingService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final SystemSettingService systemSettingService;

    // Request attribute key for email — readable by controllers
    public static final String ATTR_EMAIL = "authenticatedEmail";

    public FirebaseTokenFilter(UserService userService, SystemSettingService systemSettingService) {
        this.userService = userService;
        this.systemSettingService = systemSettingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if ("OPTIONS".equals(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/public") ||
        path.startsWith("/error")) {

        filterChain.doFilter(request, response);
        return;
    }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);
            String uid   = null;
            String email = null;

            boolean firebaseReady = !FirebaseApp.getApps().isEmpty();

            if (firebaseReady) {
                // ── Production path: full Firebase signature verification ────────
                try {
                    FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
                    uid   = decoded.getUid();
                    email = decoded.getEmail();
                } catch (Exception e) {
                    logger.error("Firebase token verification failed: " + e.getMessage());
                }
            } else {
                // ── Dev-mode path: Base64-decode JWT payload (no sig check) ─────
                try {
                    uid   = extractClaim(idToken, "user_id");
                    email = extractClaim(idToken, "email");
                    if (uid == null) uid = extractClaim(idToken, "sub");
                    logger.warn("⚠️  DEV MODE – token NOT verified. UID=" + uid + " email=" + email);
                } catch (Exception e) {
                    logger.error("Dev-mode JWT decode failed: " + e.getMessage());
                }
            }

            if (uid != null) {
                // Expose email to downstream controllers via request attribute
                if (email != null) {
                    request.setAttribute(ATTR_EMAIL, email);
                }

                // Determine role from DB
                String role = "USER";
                var userOpt = userService.getUserByFirebaseUid(uid);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole().name();
                }

                // Maintenance guard
                boolean isSuperAdmin = "sankavi8881@gmail.com".equalsIgnoreCase(email);
                if (systemSettingService.isMaintenanceMode() && !role.equals("ADMIN") && !isSuperAdmin) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"System under maintenance. Try again later.\"}");
                    return;
                }

                // ── Single-session guard ────────────────────────────────────────
                // Bypass:
                //  1. /users/session/register  — sets the token (must go through)
                //  2. /users/session/check     — heartbeat
                //  3. GET /users/{uid}         — profile fetch that fires as part
                //     of auth-state setup, right after registerSession
                boolean isSessionBypassPath = path.contains("/users/session")
                    || (request.getMethod().equals("GET") && path.matches("/api/users/[^/]+"));
                if (!isSessionBypassPath && systemSettingService.isSingleSessionEnabled()) {
                    String sessionToken = request.getHeader("X-Session-Token");
                    if (!userService.validateSession(uid, sessionToken)) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT); // 409
                        response.setContentType("application/json");
                        response.getWriter().write(
                            "{\"error\":\"SESSION_CONFLICT\",\"message\":\"Your account is logged in on another device. Please log in again.\"}"
                        );
                        return;
                    }
                }

                List<SimpleGrantedAuthority> authorities =
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

                // Store BOTH uid (principal) and email (details) in the auth token
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(uid, email, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Base64URL-decode the JWT payload and extract a single string claim. */
    private String extractClaim(String jwt, String claim) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            byte[] bytes = Base64.getUrlDecoder().decode(pad(parts[1]));
            String payload = new String(bytes, StandardCharsets.UTF_8);
            String key = "\"" + claim + "\":\"";
            int start = payload.indexOf(key);
            if (start == -1) return null;
            start += key.length();
            int end = payload.indexOf("\"", start);
            return end == -1 ? null : payload.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private String pad(String s) {
        int mod = s.length() % 4;
        if (mod == 2) return s + "==";
        if (mod == 3) return s + "=";
        return s;
    }
}
