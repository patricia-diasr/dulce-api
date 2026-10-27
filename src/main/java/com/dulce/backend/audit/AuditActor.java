package com.dulce.backend.audit;

import com.dulce.backend.auth.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

final class AuditActor {

    private AuditActor() {}

    static String current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.role().name() + "#" + user.id();
        }
        return RequestContextHolder.getRequestAttributes() != null ? "ANONYMOUS" : "SYSTEM";
    }
}
