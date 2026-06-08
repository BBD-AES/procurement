package com.bbd.procurement.global.auth;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;

public final class UserContextHolder {

    private static final ThreadLocal<UserPrincipal> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(UserPrincipal principal) {
        CONTEXT.set(principal);
    }

    public static UserPrincipal current() {
        UserPrincipal principal = CONTEXT.get();
        if (principal == null) {
        throw new ApiException(ErrorCode.AUTH_HEADER_REQUIRED);
        }
        return principal;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
