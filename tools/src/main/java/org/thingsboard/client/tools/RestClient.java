/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] bytes, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
        return execution.execute(wrapper, bytes);
    }

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

    public Optional<JsonNode> getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        Map<String, String> params = new HashMap<>();
        params.put("accessToken", accessToken);
        params.put("clientKeys", clientKeys);
        params.put("sharedKeys", sharedKeys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/v1/{accessToken}/attributes?clientKeys={clientKeys}&sharedKeys={sharedKeys}", JsonNode.class, params);
            return Optional.of(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Customer createCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
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
        Device result = restTemplate.postForEntity(baseURL + "/api/device", device, Device.class).getBody();
        return result;
    }

    public DeviceCredentials updateDeviceCredentials(DeviceId deviceId, String token) {
        DeviceCredentials deviceCredentials = getCredentials(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(token);
        return saveDeviceCredentials(deviceCredentials);
    }

    public DeviceCredentials saveDeviceCredentials(DeviceCredentials deviceCredentials) {
        return restTemplate.postForEntity(baseURL + "/api/device/credentials", deviceCredentials, DeviceCredentials.class).getBody();
    }

    public Device createDevice(Device device) {
        return restTemplate.postForEntity(baseURL + "/api/device", device, Device.class).getBody();
    }

    public Asset createAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
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

    public void deleteCustomer(CustomerId customerId) {
        restTemplate.delete(baseURL + "/api/customer/{customerId}", customerId);
    }

    public void deleteDevice(DeviceId deviceId) {
        restTemplate.delete(baseURL + "/api/device/{deviceId}", deviceId);
    }

    public void deleteAsset(AssetId assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId);
    }

    public Device assignDevice(CustomerId customerId, DeviceId deviceId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/device/{deviceId}", null, Device.class,
                customerId.toString(), deviceId.toString()).getBody();
    }

    public Asset assignAsset(CustomerId customerId, AssetId assetId) {
        return restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", HttpEntity.EMPTY, Asset.class,
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

    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/settings", adminSettings, AdminSettings.class).getBody();
    }

    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForEntity(baseURL + "/api/settings/testMail", adminSettings, AdminSettings.class);
    }

    //TODO:
//    @RequestMapping(value = "/securitySettings", method = RequestMethod.GET)
//    public SecuritySettings getSecuritySettings() {
//
//    }
    //TODO:
//    @RequestMapping(value = "/securitySettings", method = RequestMethod.POST)
//    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
//
//    }
    //TODO:
//    @RequestMapping(value = "/updates", method = RequestMethod.GET)
//    public UpdateMessage checkUpdates() {
//
//    }

    public Optional<Alarm> getAlarmById(String alarmId) {
        try {
            ResponseEntity<Alarm> alarm = restTemplate.getForEntity(baseURL + "/api/alarm/{alarmId}", Alarm.class, alarmId);
            return Optional.ofNullable(alarm.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AlarmInfo> getAlarmInfoById(String alarmId) {
        try {
            ResponseEntity<AlarmInfo> alarmInfo = restTemplate.getForEntity(baseURL + "/api/alarm/info/{alarmId}", AlarmInfo.class, alarmId);
            return Optional.ofNullable(alarmInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Alarm saveAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public void deleteAlarm(String alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}", alarmId);
    }

    public void ackAlarm(String alarmId) {
        restTemplate.postForObject(baseURL + "/api/alarm/{alarmId}/ack", new Object(), Object.class, alarmId);
    }

    public void clearAlarm(String alarmId) {
        restTemplate.postForObject(baseURL + "/api/alarm/{alarmId}/clear", new Object(), Object.class, alarmId);
    }

    public Optional<TimePageData<AlarmInfo>> getAlarms(String entityType, String entityId, String searchStatus, String status, TimePageLink pageLink, Boolean fetchOriginator) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType);
        params.put("entityId", entityId);
        params.put("searchStatus", searchStatus);
        params.put("status", status);
        params.put("fetchOriginator", String.valueOf(fetchOriginator));
        addPageLinkToParam(params, pageLink);

        StringBuilder url = new StringBuilder(baseURL);
        url.append("/api/alarm/{entityType}/{entityId}?");
        url.append("searchStatus={searchStatus}&");
        url.append("status={status}&");
        url.append("limit={limit}&");
        url.append("startTime={startTime}&");
        url.append("endTime={endTime}&");
        url.append("ascOrder={ascOrder}&");
        url.append("offset={offset}&");
        url.append("fetchOriginator={fetchOriginator}");

        try {
            ResponseEntity<TimePageData<AlarmInfo>> alarms = restTemplate.exchange(url.toString(), HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<TimePageData<AlarmInfo>>() {
            }, params);

            return Optional.ofNullable(alarms.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AlarmSeverity> getHighestAlarmSeverity(String entityType, String entityId, String searchStatus, String status) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType);
        params.put("entityId", entityId);
        params.put("searchStatus", searchStatus);
        params.put("status", status);
        try {
            ResponseEntity<AlarmSeverity> alarmSeverity = restTemplate.getForEntity(baseURL + "/api/alarm/highestSeverity/{entityType}/{entityId}?searchStatus={searchStatus}&status={status}", AlarmSeverity.class, params);
            return Optional.ofNullable(alarmSeverity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Asset> getAssetById(String assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/asset/{assetId}", Asset.class, assetId);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Asset saveAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    public void deleteAsset(String assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId);
    }

    public Optional<Asset> assignAssetToCustomer(String customerId,
                                                 String assetId) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("assetId", assetId);

        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/{customerId}/asset/{assetId}", HttpEntity.EMPTY, Asset.class, params);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Asset> unassignAssetFromCustomer(String assetId) {
        try {
            Optional<Asset> asset = getAssetById(assetId);
            restTemplate.delete(baseURL + "/api/customer/asset/{assetId}", assetId);
            return asset;
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Asset> assignAssetToPublicCustomer(String assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.postForEntity(baseURL + "/api/customer/public/asset/{assetId}", HttpEntity.EMPTY, Asset.class, assetId);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public TextPageData<Asset> getTenantAssets(TextPageLink pageLink, String type) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TextPageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/tenant/assets?limit={limit}&type={type}&textSearch{textSearch}&idOffset={idOffset}&textOffset{textOffset}",
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<TextPageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public Optional<Asset> getTenantAsset(String assetName) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, assetName);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public TextPageData<Asset> getCustomerAssets(String customerId, TextPageLink pageLink, String type) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("type", type);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TextPageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/customer/{customerId}/assets?limit={limit}&type={type}&textSearch{textSearch}&idOffset={idOffset}&textOffset{textOffset}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TextPageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public List<Asset> getAssetsByIds(String[] assetIds) {
        return restTemplate.exchange(baseURL + "/api/assets?assetIds={assetIds}", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<Asset>>() {
        }, assetIds).getBody();
    }

    public List<Asset> findByQuery(AssetSearchQuery query) {
        return restTemplate.exchange(URI.create(baseURL + "/api/assets"), HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<List<Asset>>() {
        }).getBody();
    }

    public List<EntitySubtype> getAssetTypes() {
        return restTemplate.exchange(URI.create(baseURL + "/api/asset/types"), HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<EntitySubtype>>() {
        }).getBody();
    }

    public TimePageData<AuditLog> getAuditLogsByCustomerId(String customerId, TimePageLink pageLink, String actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId);
        params.put("actionTypes", actionTypes);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TimePageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/audit/logs/customer/{customerId}?limit={limit}&startTime={startTime}&endTime={endTime}&ascOrder={ascOrder}&offset={offset}&actionTypes={actionTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TimePageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public TimePageData<AuditLog> getAuditLogsByUserId(String userId, TimePageLink pageLink, String actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);
        params.put("actionTypes", actionTypes);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TimePageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/audit/logs/user/{userId}?limit={limit}&startTime={startTime}&endTime={endTime}&ascOrder={ascOrder}&offset={offset}&actionTypes={actionTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TimePageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public TimePageData<AuditLog> getAuditLogsByEntityId(String entityType, String entityId, String actionTypes, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityType);
        params.put("entityId", entityId);
        params.put("actionTypes", actionTypes);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TimePageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/audit/logs/entity/{entityType}/{entityId}?limit={limit}&startTime={startTime}&endTime={endTime}&ascOrder={ascOrder}&offset={offset}&actionTypes={actionTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TimePageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public TimePageData<AuditLog> getAuditLogs(TimePageLink pageLink, String actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("actionTypes", actionTypes);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<TimePageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/audit/logs?limit={limit}&startTime={startTime}&endTime={endTime}&ascOrder={ascOrder}&offset={offset}&actionTypes={actionTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<TimePageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public Optional<User> getUser() {
        ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/auth/user", User.class);
        return Optional.ofNullable(user.getBody());
    }

    public void logout() {
        restTemplate.exchange(URI.create(baseURL + "/auth/logout"), HttpMethod.POST, HttpEntity.EMPTY, Object.class);
    }

    public void changePassword(JsonNode changePasswordRequest) {
        restTemplate.exchange(URI.create(baseURL + "/auth/changePassword"), HttpMethod.POST, new HttpEntity<>(changePasswordRequest), Object.class);
    }

    //TODO:
//    @RequestMapping(value = "/noauth/userPasswordPolicy", method = RequestMethod.GET)
//    public UserPasswordPolicy getUserPasswordPolicy() {
//
//    }


    public ResponseEntity<String> checkActivateToken(String activateToken) {
        return restTemplate.getForEntity(baseURL + "/noauth/activate?activateToken={activateToken}", String.class, activateToken);
    }

    public void requestResetPasswordByEmail(JsonNode resetPasswordByEmailRequest) {
        restTemplate.exchange(URI.create(baseURL + "/noauth/resetPasswordByEmail"), HttpMethod.POST, new HttpEntity<>(resetPasswordByEmailRequest), Object.class);
    }

    public ResponseEntity<String> checkResetToken(String resetToken) {
        return restTemplate.getForEntity(baseURL + "noauth/resetPassword?resetToken={resetToken}", String.class, resetToken);
    }

    public Optional<JsonNode> activateUser(JsonNode activateRequest) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/noauth/activate", activateRequest, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> resetPassword(JsonNode resetPasswordRequest) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/noauth/resetPassword", resetPasswordRequest, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }



    private void addPageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        params.put("limit", String.valueOf(pageLink.getLimit()));
        params.put("startTime", String.valueOf(pageLink.getStartTime()));
        params.put("endTime", String.valueOf(pageLink.getEndTime()));
        params.put("ascOrder", String.valueOf(pageLink.isAscOrder()));
        params.put("offset", pageLink.getIdOffset().toString());
    }

    private void addPageLinkToParam(Map<String, String> params, TextPageLink pageLink) {
        params.put("limit", String.valueOf(pageLink.getLimit()));
        params.put("textSearch", pageLink.getTextSearch());
        params.put("idOffset", pageLink.getIdOffset().toString());
        params.put("textOffset", pageLink.getTextOffset());
    }
}