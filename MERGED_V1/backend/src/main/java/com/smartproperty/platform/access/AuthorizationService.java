package com.smartproperty.platform.access;

import com.smartproperty.platform.auth.context.ActorContext;

public interface AuthorizationService {
    ActorContext requirePermission(String permissionCode);
    ActorContext requireSystemAdmin();
    boolean hasPermission(ActorContext actor, String permissionCode);
}
