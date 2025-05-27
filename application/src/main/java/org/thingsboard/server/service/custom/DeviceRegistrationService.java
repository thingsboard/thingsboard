/**
 * Copyright © 2016-2025 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DatabaseException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.sql.CustomerEntity;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class DeviceRegistrationService {

    private static final long TOKEN_EXPIRY_SECONDS = 3600;
    private final RestTemplate restTemplate;
    private final AttributesDao attributesDao;
    private final CustomerDao customerDao;
    private final DeviceDao deviceDao;
    @Value("${thingsboard.api.base-url}")
    private String baseUrl;
    @Value("${thingsboard.api.username}")
    private String username;
    @Value("${thingsboard.api.password}")
    private String password;
    private String cachedToken;
    private Instant tokenExpiryTime;

    @Autowired
    public DeviceRegistrationService(RestTemplate restTemplate, AttributesDao attributesDao, CustomerDao customerDao, DeviceDao deviceDao) {
        this.restTemplate = restTemplate;
        this.attributesDao = attributesDao;
        this.customerDao = customerDao;
        this.deviceDao = deviceDao;
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

    @SuppressWarnings("unchecked")
    public UUID findDeviceByMac(String macId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            Optional<UUID> deviceUUID = attributesDao.findDeviceIdByMacId("Mac_id", macId);
            if (deviceUUID.isPresent()) {
                return deviceUUID.get();
            }
        } catch (Exception e) {
            throw new DataValidationException("Failed to find device by MAC: " + e.getMessage(), e);
        }
        return null;
    }

    public Device findDeviceById(String deviceId) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            Optional<Device> deviceOptional = deviceDao.findDeviceById(deviceId);
            if (deviceOptional.isPresent()) {
                return deviceOptional.get();
            }
        } catch (Exception e) {
            throw new DataValidationException("Failed to find device by Device Id: " + e.getMessage(), e);
        }
        return null;
    }

    public Customer findCustomerByEmail(String email) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            Optional<Customer> customerOptional = customerDao.findCustomerByEmail(email);
            if (customerOptional.isPresent()) {
                return customerOptional.get();
            }
        } catch (Exception e) {
            throw new DataValidationException("Failed to find Customer by Email: " + e.getMessage(), e);
        }
        return null;
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
            // Step 1: Assign the device
            restTemplate.exchange(assignUrl, HttpMethod.POST, assignEntity, Void.class);
            log.info("Device '{}' assigned to customer '{}'.", deviceId, customerId);

            // Step 2: Rename device
            renameDevice(deviceId, deviceName);
            log.info("Device renamed to '{}'", deviceName);

            // Step 3: Try to create a user
            String userUrl = baseUrl + "/api/user";
            Map<String, Object> userPayload = new HashMap<>();
            userPayload.put("email", userEmail);
            userPayload.put("authority", "CUSTOMER_USER");
            userPayload.put("customerId", Map.of("entityType", "CUSTOMER", "id", customerId));

            HttpEntity<Map<String, Object>> userRequest = new HttpEntity<>(userPayload, getHeaders());

            ResponseEntity<Map> userResponse = restTemplate.postForEntity(userUrl, userRequest, Map.class);

            if (userResponse.getStatusCode().is2xxSuccessful()) {
                log.info("User '{}' created and assigned to customer '{}'", userEmail, customerId);
                return "Device assigned and user created successfully.";
            } else {
                return "Device assigned, but user creation failed.";
            }

        } catch (HttpClientErrorException.Conflict e) {
            // Email already exists
            log.warn("User with email '{}' already exists.", userEmail);
            return "Device assigned, but user already exists.";
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new IncorrectParameterException("Error during assignment: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }


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
