package com.smartproperty.platform.infrastructure.map.client;

import com.smartproperty.platform.infrastructure.map.model.GeoModels.*;

/**
 * Provider-specific boundary. Implementations own remote endpoint, key and wire-format details.
 */
public interface MapProviderClient {
    GeocodeResult geocode(GeocodeRequest request);

    ReverseGeocodeResult reverseGeocode(ReverseGeocodeRequest request);

    PoiSearchResult searchPoi(PoiSearchRequest request);

    PoiSearchResult searchNearbyPoi(NearbyPoiSearchRequest request);
}
