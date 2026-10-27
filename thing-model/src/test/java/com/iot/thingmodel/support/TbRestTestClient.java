package com.iot.thingmodel.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.thingmodel.common.Constants;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class TbRestTestClient {

    private final TestRestTemplate rest;
    private final ObjectMapper objectMapper;
    private final String tbUrl;

    public TbRestTestClient(
            TestRestTemplate rest, ObjectMapper objectMapper, String tbUrl) {
        this.rest = rest;
        this.objectMapper = objectMapper;
        this.tbUrl = stripTrailingSlash(tbUrl);
    }

    public String tbUrl() {
        return tbUrl;
    }

    public String login(String username, String password) {
        JsonNode response = post(tbUrl + "/api/auth/login", null,
                Map.of("username", username, "password", password));
        return required(response, "token");
    }

    public String tenantId(String token) {
        JsonNode user = get(tbUrl + "/api/auth/user", token);
        String tenantId = user.path("tenantId").path("id").asText();
        assertFalse(tenantId.isBlank());
        return tenantId;
    }

    public JsonNode get(String url, String token) {
        ResponseEntity<JsonNode> response = rest.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers(token)), JsonNode.class);
        assertSuccess(response, HttpMethod.GET, url);
        return response.getBody();
    }

    public JsonNode getOrNull(String url, String token) {
        try {
            ResponseEntity<JsonNode> response = rest.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers(token)), JsonNode.class);
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (HttpClientErrorException ex) {
            return null;
        }
    }

    public JsonNode post(String url, String token, Object body) {
        ResponseEntity<JsonNode> response = rest.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers(token)), JsonNode.class);
        assertSuccess(response, HttpMethod.POST, url);
        return response.getBody();
    }

    public void delete(String url, String token) {
        ResponseEntity<Void> response = rest.exchange(
                url, HttpMethod.DELETE, new HttpEntity<>(headers(token)), Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError("DELETE " + url + " -> " + response.getStatusCode());
        }
    }

    public void deleteQuietly(String url, String token) {
        try {
            delete(url, token);
        } catch (Exception ex) {
            System.err.println("cleanup failed: " + url + " -> " + ex.getMessage());
        }
    }

    public String required(JsonNode node, String field) {
        String value = node.path(field).asText();
        assertFalse(value.isBlank(), field + " is blank");
        return value;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set(Constants.Auth.HEADER_X_AUTHORIZATION,
                    Constants.Auth.BEARER_PREFIX + token);
        }
        return headers;
    }

    private void assertSuccess(
            ResponseEntity<JsonNode> response, HttpMethod method, String url) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(method + " " + url + " -> "
                    + response.getStatusCode() + " " + response.getBody());
        }
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
