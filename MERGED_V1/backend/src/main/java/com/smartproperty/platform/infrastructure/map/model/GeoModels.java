package com.smartproperty.platform.infrastructure.map.model;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class GeoModels {
    private GeoModels() {
    }

    public enum CoordinateSystem {
        GCJ_02,
        WGS_84,
        BD_09
    }

    /** Structured coordinates for business code; only the client serializes provider-specific "longitude,latitude" text. */
    public record GeoPoint(BigDecimal longitude, BigDecimal latitude, CoordinateSystem coordinateSystem) {
        public GeoPoint {
            requireNonNull(longitude, "longitude must not be null");
            requireNonNull(latitude, "latitude must not be null");
            requireNonNull(coordinateSystem, "coordinateSystem must not be null");
            if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new IllegalArgumentException("longitude must be between -180 and 180");
            }
            if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new IllegalArgumentException("latitude must be between -90 and 90");
            }
        }
    }

    public record GeocodeRequest(String address, String city) {
    }

    public record GeocodeResult(String formattedAddress, String province, String city, String district,
                                String adCode, GeoPoint location, String level) {
    }

    public record ReverseGeocodeRequest(GeoPoint location, Integer radiusMeters) {
    }

    public record ReverseGeocodeResult(String formattedAddress, String province, String city, String district,
                                       String township, String adCode, GeoPoint location) {
    }

    public record PoiSearchRequest(String keyword, String cityOrAdCode, Integer page, Integer pageSize) {
    }

    public record NearbyPoiSearchRequest(GeoPoint center, String keyword, Integer radiusMeters,
                                         Integer page, Integer pageSize) {
    }

    public record PoiSearchResult(List<PoiItem> items, long total, int page, int pageSize) {
    }

    public record PoiItem(String poiId, String name, String type, String address, String province,
                          String city, String district, String adCode, GeoPoint location) {
    }
}
