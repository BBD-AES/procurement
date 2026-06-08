package com.bbd.procurement.global.auth;

public record UserPrincipal(
        String userId,
        Role role
) {
}
