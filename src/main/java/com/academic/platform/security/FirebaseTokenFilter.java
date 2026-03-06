package com.academic.platform.security;

import com.academic.platform.service.UserService;
import com.academic.platform.service.SystemSettingService;
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
import java.util.Collections;
import java.util.List;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final SystemSettingService systemSettingService;

    public FirebaseTokenFilter(UserService userService, SystemSettingService systemSettingService) {
        this.userService = userService;
        this.systemSettingService = systemSettingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow OPTIONS requests (CORS)
        if (method.equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail();

                // Fetch real role from DB
                String role = "USER";
                var userOpt = userService.getUserByFirebaseUid(uid);
                if (userOpt.isPresent()) {
                    role = userOpt.get().getRole().name();
                }

                // --- MAINTENANCE MODE CHECK ---
                // Allow if:
                // 1. Maintenance mode is OFF
                // 2. User is ADMIN
                // 3. User is the super admin email (fail-safe)
                boolean isSuperAdmin = "sankavi8881@gmail.com".equalsIgnoreCase(email);

                if (systemSettingService.isMaintenanceMode() && !role.equals("ADMIN") && !isSuperAdmin) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    response.setContentType("application/json");
                    response.getWriter()
                            .write("{\"message\": \"System is under maintenance. Please try again later.\"}");
                    return;
                }

                List<SimpleGrantedAuthority> authorities = Collections
                        .singletonList(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(uid, null,
                        authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                logger.error("Token Error: " + e.getMessage());
            }
        } else {
            // For unauthenticated references, if strict maintenance is needed we could
            // block here too,
            // but usually public endpoints should remain open or handled by frontend
            // routing.
            // We'll trust the frontend + SecurityConfig for unauthenticated routes.
        }

        filterChain.doFilter(request, response);
    }
}
