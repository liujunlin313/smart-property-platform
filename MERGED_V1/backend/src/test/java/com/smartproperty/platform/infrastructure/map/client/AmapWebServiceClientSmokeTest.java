package com.smartproperty.platform.infrastructure.map.client;

import com.smartproperty.platform.infrastructure.map.config.MapProperties;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted opt-in smoke test. It is skipped when the local Web Service key is absent,
 * and it exercises the production client rather than a second HTTP implementation.
 */
class AmapWebServiceClientSmokeTest {
    @Test
    void callsAllFourAmapWebServiceCapabilities() {
        String key = System.getenv("AMAP_WEB_SERVICE_KEY");
        Assumptions.assumeTrue(key != null && !key.isBlank(), "AMAP_WEB_SERVICE_KEY is not configured locally");
        MapProviderClient client = new AmapWebServiceClient(
                new MapProperties("amap", new MapProperties.Amap(key, System.getenv("AMAP_WEB_SERVICE_PRIVATE_KEY"), "https://restapi.amap.com")), RestClient.builder());

        GeocodeResult geocode = client.geocode(new GeocodeRequest("北京市海淀区中关村", "北京"));
        assertNotNull(geocode.location());
        assertEquals(CoordinateSystem.GCJ_02, geocode.location().coordinateSystem());

        ReverseGeocodeResult reverse = client.reverseGeocode(new ReverseGeocodeRequest(geocode.location(), 200));
        assertNotNull(reverse.formattedAddress());
        assertFalse(reverse.formattedAddress().isBlank());

        PoiSearchResult keyword = client.searchPoi(new PoiSearchRequest("地铁站", "北京", 1, 5));
        assertFalse(keyword.items().isEmpty());

        PoiSearchResult nearby = client.searchNearbyPoi(new NearbyPoiSearchRequest(geocode.location(), "地铁站", 3000, 1, 5));
        assertFalse(nearby.items().isEmpty());
    }
}
