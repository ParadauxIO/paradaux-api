package io.paradaux.api.interceptors;

import io.paradaux.api.models.annotations.ProtectedRoute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProtectedRouteInterceptor implements HandlerInterceptor {

    private final ConcurrentMap<HandlerMethod, Boolean> protectedRouteCache = new ConcurrentHashMap<>();

    @Value("${api.secret}")
    private String secret;

    /**
     * Intercepts requests to check if they are protected routes.
     * If the route is protected, it checks for a valid secret token in the request header "X-SECRET".
     * If the secret is invalid or missing, it responds with HTTP 401 Unauthorized.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod method) {
            if (!isProtectedRoute(method)) {
                return true;
            }
            if (!hasValidSecret(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: invalid or missing secret");
                return false;
            }
        }
        return true;
    }

    /**
     * A route is protected if the method or its class is annotated with @ProtectedRoute.
     * This requires a secret token in the request header "X-SECRET".
     * */
    private boolean isProtectedRoute(HandlerMethod method) {
        return protectedRouteCache.computeIfAbsent(method, hm -> hm.hasMethodAnnotation(ProtectedRoute.class)
                || hm.getBeanType().isAnnotationPresent(ProtectedRoute.class));
    }

    /**
     * Checks if the request has a valid secret token.
     */
    public boolean hasValidSecret(HttpServletRequest request) {
        String secretHeader = request.getHeader("X-SECRET");
        return secretHeader != null && secretHeader.equals(secret);
    }
}
