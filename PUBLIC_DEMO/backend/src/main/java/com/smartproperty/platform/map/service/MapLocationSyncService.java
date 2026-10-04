package com.smartproperty.platform.map.service;

import com.smartproperty.platform.map.vo.MapLocationSyncResult;

public interface MapLocationSyncService {
    MapLocationSyncResult syncMissingLocations();
}
