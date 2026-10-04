package com.smartproperty.platform.auth.context;

import java.util.Set;

/** Current caller abstraction; later this will be populated from SecurityContext. */
public record ActorContext(
        Long accountId,
        Long personId,
        String username,
        String actorType,
        Set<String> roles,
        Long agencyId
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
