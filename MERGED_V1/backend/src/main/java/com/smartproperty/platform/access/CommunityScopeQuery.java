package com.smartproperty.platform.access;

import java.util.List;

/** Internal query contract; services overwrite scope fields from the authenticated actor. */
public interface CommunityScopeQuery {
    void setScopeRestricted(boolean scopeRestricted);
    void setScopedCommunityIds(List<Long> scopedCommunityIds);
}
