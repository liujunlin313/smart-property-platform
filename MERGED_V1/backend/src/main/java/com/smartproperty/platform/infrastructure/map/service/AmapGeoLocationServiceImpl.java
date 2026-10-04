package com.smartproperty.platform.infrastructure.map.service;
import com.smartproperty.platform.infrastructure.map.client.MapProviderClient;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.*;
import org.springframework.stereotype.Service;

@Service
public class AmapGeoLocationServiceImpl implements GeoLocationService {
    private final MapProviderClient providerClient;

    public AmapGeoLocationServiceImpl(MapProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    @Override
    public GeocodeResult geocode(GeocodeRequest request) {
        return providerClient.geocode(request);
    }

    @Override
    public ReverseGeocodeResult reverseGeocode(ReverseGeocodeRequest request) {
        return providerClient.reverseGeocode(request);
    }

    @Override
    public PoiSearchResult searchPoi(PoiSearchRequest request) {
        return providerClient.searchPoi(request);
    }

    @Override
    public PoiSearchResult searchNearbyPoi(NearbyPoiSearchRequest request) {
        return providerClient.searchNearbyPoi(request);
    }
}
