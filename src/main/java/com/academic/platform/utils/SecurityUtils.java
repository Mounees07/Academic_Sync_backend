package com.academic.platform.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /** Returns the Firebase UID of the currently authenticated user (stored as principal). */
    public String getCurrentUserUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof String uid && !"anonymousUser".equals(uid)) {
            return uid;
        }
        return null;
    }

    public String getCurrentUserEmail() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Object emailAttr = attributes.getRequest().getAttribute("authenticatedEmail");
                if (emailAttr instanceof String email && !email.isBlank()) {
                    return email;
                }
            }
        } catch (Exception e) {}

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object credentials = auth.getCredentials();
        if (credentials instanceof String email && !email.isBlank()) {
            return email;
        }
        return null;
    }
}
