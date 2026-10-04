package com.smartproperty.platform.infrastructure.map.client;

import com.smartproperty.platform.infrastructure.map.config.MapProperties;
import com.smartproperty.platform.infrastructure.map.exception.MapProviderNotConfiguredException;
import com.smartproperty.platform.infrastructure.map.exception.MapProviderRequestException;
import com.smartproperty.platform.infrastructure.map.exception.GeoLocationNotFoundException;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The only layer that knows AMap endpoints, Web Service key and provider response conventions.
 */
@Component
public class AmapWebServiceClient implements MapProviderClient {
    private final MapProperties properties;
    private final RestClient restClient;
    private final AmapRequestSigner requestSigner = new AmapRequestSigner();

    public AmapWebServiceClient(MapProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    private void requireConfiguration() {
        if (!properties.configured()) {
            throw new MapProviderNotConfiguredException();
        }
    }

    @Override
    public GeocodeResult geocode(GeocodeRequest request) {
        requireConfiguration();
        if (request == null || request.address() == null || request.address().isBlank()) {
            throw new IllegalArgumentException("地理编码地址不能为空");
        }
        JsonNode root = get("/v3/geocode/geo", params -> {
            params.put("address", request.address());
            addOptionalQueryParam(params, "city", request.city());
        });
        JsonNode geocodes = root.path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            throw new GeoLocationNotFoundException("高德未返回可用的地理编码结果");
        }
        JsonNode value = geocodes.get(0);
        return new GeocodeResult(
                text(value, "formatted_address"), text(value, "province"), normalizedText(value, "city"),
                text(value, "district"), text(value, "adcode"), toGcj02Point(value, "location"), text(value, "level"));
    }

    @Override
    public ReverseGeocodeResult reverseGeocode(ReverseGeocodeRequest request) {
        requireConfiguration();
        if (request == null || request.location() == null) {
            throw new IllegalArgumentException("逆地理编码坐标不能为空");
        }
        JsonNode root = get("/v3/geocode/regeo", params -> {
            params.put("location", toAmapLocation(request.location()));
            if (request.radiusMeters() != null) {
                params.put("radius", String.valueOf(request.radiusMeters()));
            }
            params.put("extensions", "base");
        });
        JsonNode value = root.path("regeocode");
        if (value.isMissingNode() || value.isNull()) {
            throw new GeoLocationNotFoundException("高德未返回可用的逆地理编码结果");
        }
        JsonNode component = value.path("addressComponent");
        return new ReverseGeocodeResult(
                text(value, "formatted_address"), text(component, "province"), normalizedText(component, "city"),
                text(component, "district"), text(component, "township"), text(component, "adcode"),
                request.location());
    }

    @Override
    public PoiSearchResult searchPoi(PoiSearchRequest request) {
        requireConfiguration();
        if (request == null || request.keyword() == null || request.keyword().isBlank()) {
            throw new IllegalArgumentException("POI 关键字不能为空");
        }
        int page = normalizedPage(request.page());
        int pageSize = normalizedPageSize(request.pageSize());
        JsonNode root = get("/v3/place/text", params -> {
            params.put("keywords", request.keyword());
            addOptionalQueryParam(params, "city", request.cityOrAdCode());
            params.put("page", String.valueOf(page));
            params.put("offset", String.valueOf(pageSize));
            params.put("extensions", "base");
        });
        return toPoiSearchResult(root, page, pageSize);
    }

    @Override
    public PoiSearchResult searchNearbyPoi(NearbyPoiSearchRequest request) {
        requireConfiguration();
        if (request == null || request.center() == null || request.keyword() == null || request.keyword().isBlank()) {
            throw new IllegalArgumentException("周边 POI 查询需要中心坐标和关键字");
        }
        int page = normalizedPage(request.page());
        int pageSize = normalizedPageSize(request.pageSize());
        int radius = normalizedRadius(request.radiusMeters());
        JsonNode root = get("/v3/place/around", params -> {
            params.put("location", toAmapLocation(request.center()));
            params.put("keywords", request.keyword());
            params.put("radius", String.valueOf(radius));
            params.put("page", String.valueOf(page));
            params.put("offset", String.valueOf(pageSize));
            params.put("extensions", "base");
        });
        return toPoiSearchResult(root, page, pageSize);
    }

    private JsonNode get(String path, java.util.function.Consumer<Map<String, String>> query) {
        try {
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("key", properties.webServiceKey());
            query.accept(parameters);
            String signature = requestSigner.sign(parameters, properties.webServicePrivateKey());
            if (signature != null) {
                parameters.put("sig", signature);
            }
            JsonNode body = restClient.get().uri(builder -> {
                builder.path(path);
                parameters.forEach(builder::queryParam);
                return builder.build();
            }).retrieve().body(JsonNode.class);
            if (body == null) {
                throw new MapProviderRequestException("高德服务未返回响应体");
            }
            if (!"1".equals(text(body, "status"))) {
                throw new MapProviderRequestException("高德服务请求失败：" + safeProviderMessage(body));
            }
            return body;
        } catch (MapProviderRequestException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new MapProviderRequestException("高德服务网络请求失败", exception);
        }
    }

    private PoiSearchResult toPoiSearchResult(JsonNode root, int page, int pageSize) {
        List<PoiItem> items = new ArrayList<>();
        JsonNode pois = root.path("pois");
        if (pois.isArray()) {
            for (JsonNode poi : pois) {
                GeoPoint location = poi.hasNonNull("location") ? toGcj02Point(poi, "location") : null;
                items.add(new PoiItem(text(poi, "id"), text(poi, "name"), text(poi, "type"), text(poi, "address"),
                        text(poi, "pname"), text(poi, "cityname"), text(poi, "adname"), text(poi, "adcode"), location));
            }
        }
        return new PoiSearchResult(List.copyOf(items), longValue(root, "count"), page, pageSize);
    }

    private GeoPoint toGcj02Point(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            throw new GeoLocationNotFoundException("高德返回结果缺少坐标");
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new MapProviderRequestException("高德返回的坐标格式无效");
        }
        try {
            return new GeoPoint(new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim()), CoordinateSystem.GCJ_02);
        } catch (NumberFormatException exception) {
            throw new MapProviderRequestException("高德返回的坐标数值无效", exception);
        }
    }

    private String toAmapLocation(GeoPoint point) {
        return point.longitude().setScale(6, RoundingMode.HALF_UP).toPlainString()
                + "," + point.latitude().setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    private void addOptionalQueryParam(Map<String, String> parameters, String name, String value) {
        if (value != null && !value.isBlank()) {
            parameters.put(name, value);
        }
    }

    private int normalizedPage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizedPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 25);
    }

    private int normalizedRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return 1000;
        }
        return Math.max(0, Math.min(radiusMeters, 50000));
    }

    private long longValue(JsonNode node, String field) {
        try {
            return Long.parseLong(text(node, field));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String safeProviderMessage(JsonNode body) {
        String info = text(body, "info");
        String infocode = text(body, "infocode");
        return (info == null ? "未知错误" : info) + (infocode == null ? "" : "（" + infocode + "）");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private String normalizedText(JsonNode node, String field) {
        String value = text(node, field);
        return value == null || value.isBlank() || "[]".equals(value) ? null : value;
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);
        return factory;
    }
}
