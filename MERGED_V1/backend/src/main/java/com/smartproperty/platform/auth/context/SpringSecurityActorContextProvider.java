package com.smartproperty.platform.auth.context;

import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** Builds the Stage 1 actor abstraction from the authenticated JWT. */
@Component
public class SpringSecurityActorContextProvider implements ActorContextProvider {
    @Override
    public ActorContext currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "请先登录后再访问");
        }
        Long accountId = asLong(jwt.getSubject());
        Long personId = asLong(jwt.getClaimAsString("personId"));
        String username = jwt.getClaimAsString("username");
        Set<String> roles = new LinkedHashSet<>(jwt.getClaimAsStringList("roles") == null
                ? Set.of() : jwt.getClaimAsStringList("roles"));
        return new ActorContext(accountId, personId, username == null ? authentication.getName() : username,
                "ACCOUNT", Set.copyOf(roles), asLong(jwt.getClaimAsString("agencyId")));
    }

    private Long asLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
