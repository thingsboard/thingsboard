/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.client.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@RequiredArgsConstructor
public class RestClient implements ClientHttpRequestInterceptor {
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private final RestTemplate restTemplate = new RestTemplate();
    private String token;
    private final String baseURL;

    public void login(String username, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = restTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        this.token = tokenInfo.getBody().get("token").asText();
        restTemplate.setInterceptors(Collections.singletonList(this));
    }

    public Optional<Device> findDevice(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("deviceName", name);
        try {
            ResponseEntity<Device> deviceEntity = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, params);
            return Optional.of(deviceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Customer> findCustomer(String title) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("customerTitle", title);
        try {
            ResponseEntity<Customer> customerEntity = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, params);
            return Optional.of(customerEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Asset> findAsset(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("assetName", name);
        try {
            ResponseEntity<Asset> assetEntity = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, params);
            return Optional.of(assetEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    public Device createDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/device", device, Device.class).getBody();
    }

    public Asset createAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    public Alarm createAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public Device assignDevice(CustomerId customerId, DeviceId deviceId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class,
                customerId.toString(), deviceId.toString()).getBody();
    }

    public Asset assignAsset(CustomerId customerId, AssetId assetId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", null, Asset.class,
                customerId.toString(), assetId.toString()).getBody();
    }

    public EntityRelation makeRelation(String relationType, EntityId idFrom, EntityId idTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(idFrom);
        relation.setTo(idTo);
        relation.setType(relationType);
        return restTemplate.postForEntity(baseURL + "/api/relation", relation, EntityRelation.class).getBody();
    }

    public DeviceCredentials getCredentials(DeviceId id) {
        return restTemplate.getForEntity(baseURL + "/api/device/" + id.getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public String getToken() {
        return token;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] bytes, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
        return execution.execute(wrapper, bytes);
    }
}