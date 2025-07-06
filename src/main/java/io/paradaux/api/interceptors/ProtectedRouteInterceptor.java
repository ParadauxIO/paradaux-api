package io.paradaux.api.interceptors;

import io.paradaux.api.models.annotations.ProtectedRoute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ProtectedRouteInterceptor implements HandlerInterceptor {

    @Value("${api.secret}")
    private String secret;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;

            boolean methodProtected = method.hasMethodAnnotation(ProtectedRoute.class);
            boolean classProtected = method.getBeanType().isAnnotationPresent(ProtectedRoute.class);

            if (methodProtected || classProtected) {
                String secretHeader = request.getHeader("X-SECRET");
                if (secretHeader == null || !secretHeader.equals(secret)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Unauthorized: invalid or missing secret");
                    return false;
                }
            }
        }
        return true;
    }
}
