package com.smartproperty.platform.access;

import java.util.List;

/** Global means unrestricted. A non-global scope may intentionally contain no communities. */
public record CommunityDataScope(boolean global, List<Long> communityIds) {
    public CommunityDataScope { communityIds = communityIds == null ? List.of() : List.copyOf(communityIds); }
    public boolean allows(Long communityId) { return global || (communityId != null && communityIds.contains(communityId)); }
}
