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

    /**
     * Returns the email of the currently authenticated user.
     * FirebaseTokenFilter stores the email as the credentials value.
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object credentials = auth.getCredentials();
        if (credentials instanceof String email && !email.isBlank()) {
            return email;
        }
        return null;
    }
}
