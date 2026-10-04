package com.smartproperty.platform.infrastructure.map.service;

import com.smartproperty.platform.infrastructure.map.model.GeoModels.*;

/** Business-facing geographical service. Controllers remain a future concern. */
public interface GeoLocationService {
    GeocodeResult geocode(GeocodeRequest request);

    ReverseGeocodeResult reverseGeocode(ReverseGeocodeRequest request);

    PoiSearchResult searchPoi(PoiSearchRequest request);

    PoiSearchResult searchNearbyPoi(NearbyPoiSearchRequest request);
}
