package com.smartproperty.platform.map.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Explicit manual smoke: it writes only missing map coordinates in the configured local database. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "MAP_API_SMOKE", matches = "true")
class MapApiSmokeTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    private String accessToken;

    @Test
    void syncsOnlyMissingLocationsThenReturnsDatabaseBackedScene() throws Exception {
        accessToken = loginAsBootstrapAdmin();
        JsonNode first = data(post("/api/admin/map/locations/sync-missing"));
        assertEquals(0, first.path("failed").asInt(), "定位失败时不得伪造坐标；请查看真实 Provider 返回");
        assertTrue(first.path("updated").asInt() >= 0);

        JsonNode second = data(post("/api/admin/map/locations/sync-missing"));
        assertEquals(0, second.path("requested").asInt(), "第二次补全不应再次请求已写入的楼栋");
        assertEquals(0, second.path("updated").asInt(), "第二次补全不应覆盖已有坐标");

        JsonNode scene = data(get("/api/admin/map/scene"));
        assertTrue(scene.path("communities").isArray());
        assertTrue(scene.path("buildings").isArray());
        assertTrue(scene.path("meta").path("locatedBuildingCount").asInt() > 0);
    }

    private String loginAsBootstrapAdmin() throws Exception {
        String username = System.getenv("APP_BOOTSTRAP_ADMIN_USERNAME");
        String password = System.getenv("APP_BOOTSTRAP_ADMIN_PASSWORD");
        assertNotNull(username, "本机 bootstrap 管理员用户名未配置");
        assertNotNull(password, "本机 bootstrap 管理员密码未配置");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(java.util.Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.path("success").asBoolean(), response.path("message").asText());
        String token = response.path("data").path("accessToken").asText();
        assertFalse(token.isBlank(), "登录未返回 access token");
        return token;
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        request.header("Authorization", "Bearer " + accessToken);
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        JsonNode envelope = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(envelope.path("success").asBoolean(), envelope.path("message").asText());
        return envelope.path("data");
    }
}
