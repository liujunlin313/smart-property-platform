package com.smartproperty.platform.infrastructure.map.client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

/** Generates one AMap Web Service sig per request; private key material is never returned or logged. */
final class AmapRequestSigner {
    String sign(Map<String, String> requestParameters, String privateKey) {
        if (privateKey == null || privateKey.isBlank()) {
            return null;
        }
        String canonical = requestParameters.entrySet().stream()
                .filter(entry -> !"sig".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return md5Hex(canonical + privateKey);
    }

    private String md5Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(32);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 MD5 请求签名", exception);
        }
    }
}
