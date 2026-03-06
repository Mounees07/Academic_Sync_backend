package com.academic.platform.interceptor;

import com.academic.platform.service.SystemSettingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    @Autowired
    private SystemSettingService systemSettingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Allow admin API, login, and public resources
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") ||
                path.startsWith("/api/admin") ||
                path.equals("/error")) {
            return true;
        }

        if (systemSettingService.isMaintenanceMode()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"message\": \"System is under maintenance\"}");
            return false;
        }
        return true;
    }
}
