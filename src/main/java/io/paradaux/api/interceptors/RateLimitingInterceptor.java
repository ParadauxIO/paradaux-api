package io.paradaux.api.interceptors;

import io.paradaux.api.services.RateLimitingService;
import io.paradaux.api.utils.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimiterService;
    private final ProtectedRouteInterceptor protectedRouteInterceptor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String ip = IPUtils.getClientIp(request);

        // Allow requests that contain a valid secret token
        if (protectedRouteInterceptor.hasValidSecret(request)) {
            return true;
        }

        if (!rateLimiterService.isAllowed(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded");
            return false;
        }

        return true;
    }
}
