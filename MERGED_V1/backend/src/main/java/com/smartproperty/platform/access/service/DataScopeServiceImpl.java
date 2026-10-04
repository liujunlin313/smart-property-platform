package com.smartproperty.platform.access.service;

import com.smartproperty.platform.access.*;
import com.smartproperty.platform.access.mapper.AccessControlMapper;
import com.smartproperty.platform.auth.context.ActorContext;
import com.smartproperty.platform.auth.context.ActorContextProvider;
import com.smartproperty.platform.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DataScopeServiceImpl implements DataScopeService {
    private final ActorContextProvider actorContextProvider; private final AccessControlMapper mapper;
    public DataScopeServiceImpl(ActorContextProvider actorContextProvider, AccessControlMapper mapper) { this.actorContextProvider = actorContextProvider; this.mapper = mapper; }
    @Override public CommunityDataScope currentCommunityScope() {
        ActorContext actor = actorContextProvider.currentActor();
        if (actor.hasRole("SYSTEM_ADMIN")) return new CommunityDataScope(true, List.of());
        return actor.accountId() == null ? new CommunityDataScope(false, List.of()) : new CommunityDataScope(false, mapper.selectActiveCommunityScopeIds(actor.accountId()));
    }
    @Override public <T extends CommunityScopeQuery> T apply(T query) { CommunityDataScope scope = currentCommunityScope(); query.setScopeRestricted(!scope.global()); query.setScopedCommunityIds(scope.communityIds()); return query; }
    @Override public void requireCommunity(Long communityId) { require(currentCommunityScope(), communityId); }
    @Override public void requireProperty(Long id) { require(currentCommunityScope(), mapper.selectCommunityIdForProperty(id)); }
    @Override public void requireBill(Long id) { require(currentCommunityScope(), mapper.selectCommunityIdForBill(id)); }
    @Override public void requireResidence(Long id) { require(currentCommunityScope(), mapper.selectCommunityIdForResidence(id)); }
    @Override public void requireBuilding(Long id) { require(currentCommunityScope(), mapper.selectCommunityIdForBuilding(id)); }
    @Override public void requireCurrentResidentPerson(Long id) { require(currentCommunityScope(), mapper.selectCommunityIdForPersonCurrentResidence(id)); }
    private void require(CommunityDataScope scope, Long communityId) { if (!scope.allows(communityId)) throw new BusinessException(HttpStatus.FORBIDDEN, "DATA_SCOPE_DENIED", "当前账号无权访问该小区范围的数据"); }
}
