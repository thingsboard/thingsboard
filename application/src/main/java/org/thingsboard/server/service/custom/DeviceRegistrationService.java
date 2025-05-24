/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.custom;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DatabaseException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

@Service
@Slf4j
public class DeviceRegistrationService {

    @Value("${thingsboard.api.base-url}")
    private String baseUrl;

    @Value("${thingsboard.api.username}")
    private String username;

    @Value("${thingsboard.api.password}")
    private String password;

    private static final long TOKEN_EXPIRY_SECONDS = 3600;

    private String cachedToken;
    private Instant tokenExpiryTime;

    private final RestTemplate restTemplate;

    @Autowired
    public DeviceRegistrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Thread-safe token getter with caching
    private synchronized String loginAndGetToken() {
        if (cachedToken != null && tokenExpiryTime != null && Instant.now().isBefore(tokenExpiryTime)) {
            return cachedToken;
        }
        return fetchNewToken();
    }

    // Separate method for fetching token from ThingsBoard
    private String fetchNewToken() {
        String loginUrl = baseUrl + "/api/auth/login";

        JSONObject loginJson = new JSONObject();
        loginJson.put("username", username);
        loginJson.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(loginJson.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(loginUrl, HttpMethod.POST, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = new JSONObject(response.getBody());
                cachedToken = jsonResponse.getString("token");
                tokenExpiryTime = Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS - 30);
                return cachedToken;
            } else {
                throw new RuntimeException("Login failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception during login: " + e.getMessage(), e);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginAndGetToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public boolean isValidMac(String macId) {
        return findDeviceByMac(macId).isPresent();
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> findDeviceByMac(String macId) {
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

        int page = 0;
        int pageSize = 100;

        try {
            while (true) {
                String listDevicesUrl = baseUrl + "/api/tenant/devices?pageSize=" + pageSize + "&page=" + page;
                ResponseEntity<Map> response = restTemplate.exchange(listDevicesUrl, HttpMethod.GET, entity, Map.class);

                List<Map<String, Object>> devices = (List<Map<String, Object>>) response.getBody().get("data");

                if (devices == null || devices.isEmpty()) {
                    break;
                }

                for (Map<String, Object> device : devices) {
                    String deviceId = (String) ((Map<String, Object>) device.get("id")).get("id");
                    if (hasMatchingAttribute(deviceId, "DEVICE", "Mac_id", macId)) {
                        return Optional.of(device);
                    }
                }

                page++; // Next page
            }
        } catch (Exception e) {
            throw new DataValidationException("Failed to find device by MAC: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    public Optional<Map<String, Object>> findCustomerByEmail(String email) {
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
        int page = 0;
        int pageSize = 100;

        try {
            while (true) {
                String url = baseUrl + "/api/customers?pageSize=" + pageSize + "&page=" + page;
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                List<Map<String, Object>> customers = (List<Map<String, Object>>) response.getBody().get("data");

                if (customers == null || customers.isEmpty()) {
                    break;
                }

                for (Map<String, Object> customer : customers) {
                    if (email.equalsIgnoreCase((String) customer.get("title"))) {
                        return Optional.of(customer);
                    }

                    String customerId = (String) ((Map<String, Object>) customer.get("id")).get("id");
                    if (hasMatchingAttribute(customerId, "CUSTOMER", "email", email)) {
                        return Optional.of(customer);
                    }
                }

                page++;
            }
        } catch (Exception e) {
            throw new DataValidationException("Failed to find customer by email: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private boolean hasMatchingAttribute(String entityId, String entityType, String key, String expectedValue) {
        String attrUrl = baseUrl + "/api/plugins/telemetry/" + entityType + "/" + entityId + "/values/attributes/SERVER_SCOPE";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

        ResponseEntity<List> response = restTemplate.exchange(attrUrl, HttpMethod.GET, entity, List.class);
        List<Map<String, Object>> attributes = response.getBody();

        for (Map<String, Object> attr : attributes) {
            if (key.equalsIgnoreCase((String) attr.get("key")) && expectedValue.equalsIgnoreCase((String) attr.get("value"))) {
                return true;
            }
        }
        return false;
    }

    public Customer createCustomer(String email) {
        HttpHeaders headers = getHeaders();

        Map<String, Object> customerRequest = new HashMap<>();
        customerRequest.put("title", email);
        customerRequest.put("email", email);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerRequest, headers);

        try {
            String createCustomerUrl = baseUrl + "/api/customer";
            ResponseEntity<Map> response = restTemplate.exchange(createCustomerUrl, HttpMethod.POST, entity, Map.class);

            Map<String, Object> body = response.getBody();
            Map<String, Object> idMap = (Map<String, Object>) body.get("id");

            Customer customer = new Customer();
            customer.setId(new CustomerId(UUID.fromString((String) idMap.get("id"))));
            customer.setTitle((String) body.get("title"));
            customer.setEmail((String) body.get("email"));
            return customer;

        } catch (Exception e) {
            throw new DatabaseException("Failed to create customer: " + e.getMessage(), e);
        }
    }

    public String assignDeviceToCustomer(String deviceId, String customerId, String deviceName, String userEmail) {
        String assignUrl = baseUrl + "/api/customer/" + customerId + "/device/" + deviceId;
        HttpEntity<Void> assignEntity = new HttpEntity<>(getHeaders());

        try {
            restTemplate.exchange(assignUrl, HttpMethod.POST, assignEntity, Void.class);
            log.info("Device '{}' assigned to customer '{}'.", deviceId, customerId);

            renameDevice(deviceId, deviceName);
            log.info("Call to updateDeviceName");

            // Step 2: Create user associated with customer
            String userUrl = baseUrl + "/api/user";
            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("email", userEmail);
            userPayload.put("authority", "CUSTOMER_USER");
            userPayload.put("customerId", Map.of("entityType", "CUSTOMER", "id", customerId));

            HttpEntity<Map<String, Object>> userRequest = new HttpEntity<>(userPayload, getHeaders());
            ResponseEntity<Map> userResponse = restTemplate.postForEntity(userUrl, userRequest, Map.class);

            if (userResponse.getStatusCode().is2xxSuccessful()) {
                return "Customer and user created successfully.";
            } else {
                return "Customer created, but failed to create user.";
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new IncorrectParameterException("Error assigning device: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while assigning device: " + e.getMessage(), e);
        }
    }
//
//    public String createCustomerAndUser(String customerName, String userEmail) {
//        try {
//            // Step 1: Create customer
//            String customerUrl = baseUrl + "/api/customer";
//            Map<String, String> customerPayload = Map.of("title", customerName);
//            HttpEntity<Map<String, String>> customerRequest = new HttpEntity<>(customerPayload, headers);
//
//            ResponseEntity<Map> customerResponse = restTemplate.postForEntity(customerUrl, customerRequest, Map.class);
//            Map<String, Object> customerData = customerResponse.getBody();
//            if (customerData == null || !customerData.containsKey("id")) {
//                return "Failed to create customer.";
//            }
//
//            String customerId = ((Map<String, String>) customerData.get("id")).get("id");
//
//            // Step 2: Create user associated with customer
//            String userUrl = baseUrl + "/api/user";
//            Map<String, Object> userPayload = new HashMap<>();
//            userPayload.put("email", userEmail);
//            userPayload.put("authority", "CUSTOMER_USER");
//            userPayload.put("customerId", Map.of("entityType", "CUSTOMER", "id", customerId));
//
//            HttpEntity<Map<String, Object>> userRequest = new HttpEntity<>(userPayload, headers);
//            ResponseEntity<Map> userResponse = restTemplate.postForEntity(userUrl, userRequest, Map.class);
//
//            if (userResponse.getStatusCode().is2xxSuccessful()) {
//                return "Customer and user created successfully.";
//            } else {
//                return "Customer created, but failed to create user.";
//            }
//
//        } catch (HttpClientErrorException | HttpServerErrorException ex) {
//            return "API error: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString();
//        } catch (Exception ex) {
//            return "Unexpected error: " + ex.getMessage();
//        }
//    }


    public void renameDevice(String deviceId, String newName) {
        // 1) GET the existing device
        String getUrl = baseUrl + "/api/device/" + deviceId;
        Device device = restTemplate.exchange(
                getUrl,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                Device.class
        ).getBody();

        if (device == null) {
            throw new IncorrectParameterException("Device not found: " + deviceId);
        }

        // 2) change the name
        device.setName(newName);

        // 3) POST the modified object back to /api/device
        String saveUrl = baseUrl + "/api/device";
        restTemplate.exchange(
                saveUrl,
                HttpMethod.POST,
                new HttpEntity<>(device, getHeaders()),
                Void.class
        );
    }


    public String getDeviceAccessToken(String deviceId) {
        String url = baseUrl + "/api/device/" + deviceId + "/credentials";
        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("credentialsId")) {
                return (String) body.get("credentialsId");
            } else {
                throw new DeviceCredentialsValidationException("Access token not found in response for device: " + deviceId);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new DeviceCredentialsValidationException("Error fetching device token: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while fetching device token: " + e.getMessage(), e);
        }
    }

}
