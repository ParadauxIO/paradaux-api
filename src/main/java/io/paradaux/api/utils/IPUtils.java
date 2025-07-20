package io.paradaux.api.utils;

import jakarta.servlet.http.HttpServletRequest;

public class IPUtils {

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim(); // real client IP
        }
        return request.getRemoteAddr(); // fallback
    }
}
