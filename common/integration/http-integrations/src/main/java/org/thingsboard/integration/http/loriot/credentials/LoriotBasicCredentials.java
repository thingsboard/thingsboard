// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.loriot.credentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.integration.template.FormFieldType;
import org.thingsboard.server.common.data.integration.template.TemplateField;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Data
public class LoriotBasicCredentials implements LoriotCredentials, ClientHttpRequestInterceptor {

    @TemplateField(key = "loriotUsername", label = "LORIOT username", secret = true, group = "LORIOT Connection", required = true)
    private String username;

    @TemplateField(key = "loriotPassword", label = "LORIOT password", type = FormFieldType.PASSWORD, secret = true, group = "LORIOT Connection", required = true)
    private String password;

    @JsonIgnore
    private String session;

    @JsonIgnore
    private RestTemplate restTemplate;

    @JsonIgnore
    private String baseUrl;

    @Override
    public void setInterceptor(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        refreshSession(restTemplate, baseUrl);
    }

    private void refreshSession(RestTemplate restTemplate, String baseUrl) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("user", username);
        loginRequest.put("pwd", password);
        ResponseEntity<JsonNode> sessionInfo = restTemplate.postForEntity(baseUrl + "1/pub/login", loginRequest, JsonNode.class);
        if (sessionInfo.getStatusCode().equals(HttpStatus.OK)) {
            session = sessionInfo.getBody().get("session").asText();
            restTemplate.getInterceptors().add(this);
        } else {
            throw new RuntimeException(sessionInfo.getBody().get("error").asText());
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
        ClientHttpResponse response = execution.execute(wrapper, body);
        if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
            synchronized (this) {
                restTemplate.getInterceptors().remove(this);
                refreshSession(restTemplate, baseUrl);
                wrapper.getHeaders().set(AUTH_HEADER_PARAM, "Session " + session);
                return execution.execute(wrapper, body);
            }
        }
        return response;
    }
}
