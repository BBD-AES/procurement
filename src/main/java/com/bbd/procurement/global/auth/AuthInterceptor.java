package com.bbd.procurement.global.auth;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String userId = request.getHeader(HEADER_USER_ID);
        String roleHeader = request.getHeader(HEADER_USER_ROLE);

        if (userId == null || userId.isBlank() || roleHeader == null || roleHeader.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_HEADER_REQUIRED);
        }

        Role role = parseRole(roleHeader);
        UserContextHolder.set(new UserPrincipal(userId, role));

        HasRole annotation = resolveHasRole(handlerMethod);
        if (annotation != null) {
            checkRole(annotation.value(), role);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContextHolder.clear();
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.AUTH_ROLE_INVALID);
        }
    }

    private HasRole resolveHasRole(HandlerMethod handlerMethod) {
        HasRole methodAnnotation = handlerMethod.getMethodAnnotation(HasRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(HasRole.class);
    }

    private void checkRole(Role[] allowedRoles, Role userRole) {
        boolean allowed = Arrays.stream(allowedRoles).anyMatch(r -> r == userRole);
        if (!allowed) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }
}
