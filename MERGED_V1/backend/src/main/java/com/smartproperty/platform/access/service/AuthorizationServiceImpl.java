package com.smartproperty.platform.access.service;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.mapper.AccessControlMapper;
import com.smartproperty.platform.auth.context.ActorContext;
import com.smartproperty.platform.auth.context.ActorContextProvider;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final ActorContextProvider actorContextProvider;
    private final AccessControlMapper mapper;
    public AuthorizationServiceImpl(ActorContextProvider actorContextProvider, AccessControlMapper mapper) { this.actorContextProvider = actorContextProvider; this.mapper = mapper; }
    @Override public ActorContext requirePermission(String permissionCode) {
        ActorContext actor = actorContextProvider.currentActor();
        if (!hasPermission(actor, permissionCode)) throw new BusinessException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "当前账号没有执行此操作的权限");
        return actor;
    }
    @Override public ActorContext requireSystemAdmin() {
        ActorContext actor = actorContextProvider.currentActor();
        if (!actor.hasRole("SYSTEM_ADMIN")) throw new BusinessException(HttpStatus.FORBIDDEN, "SYSTEM_ADMIN_REQUIRED", "该操作仅系统管理员可执行");
        return actor;
    }
    @Override public boolean hasPermission(ActorContext actor, String permissionCode) {
        return actor != null && (actor.hasRole("SYSTEM_ADMIN") || (actor.accountId() != null && mapper.selectPermissionCodes(actor.accountId()).contains(permissionCode)));
    }
}
