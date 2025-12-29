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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.HeaderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.internal.ValidatableResponseImpl;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.thingsboard.server.common.data.StringUtils.isEmpty;

public class TestRestClient {
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final RequestSpecification requestSpec;
    private String token;
    private String refreshToken;

    public TestRestClient(String url) {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        requestSpec = given().baseUri(url)
                .contentType(ContentType.JSON)
                .config(RestAssuredConfig.config()
                        .headerConfig(HeaderConfig.headerConfig()
                                .overwriteHeadersWithName(JWT_TOKEN_HEADER_PARAM, CONTENT_TYPE_HEADER)));

        if (url.matches("^(https)://.*$")) {
            requestSpec.relaxedHTTPSValidation();
        }
    }

    public void login(String username, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        JsonPath jsonPath = given().spec(requestSpec).body(loginRequest)
                .post("/api/auth/login")
                .getBody().jsonPath();
        token = jsonPath.get("token");
        refreshToken = jsonPath.get("refreshToken");
        requestSpec.header(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
    }

    public void resetToken() {
        token = null;
        refreshToken = null;
    }

    public Tenant postTenant(Tenant tenant) {
        return given().spec(requestSpec).body(tenant)
                .post("/api/tenant")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Tenant.class);
    }

    public Device postDevice(String accessToken, Device device) {
        return given().spec(requestSpec).body(device)
                .pathParams("accessToken", accessToken)
                .post("/api/device?accessToken={accessToken}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public ObjectNode postRpcLwm2mParams(String deviceIdStr, String body) {
        return given().spec(requestSpec).body(body)
                .post("/api/plugins/rpc/twoway/" + deviceIdStr)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(ObjectNode.class);
    }

    public CalculatedField postCalculatedField(CalculatedField calculatedField) {
        return given().spec(requestSpec).body(calculatedField)
                .post("/api/calculatedField")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(CalculatedField.class);
    }

    public Device getDeviceByName(String deviceName) {
        return given().spec(requestSpec).pathParam("deviceName", deviceName)
                .get("/api/tenant/devices?deviceName={deviceName}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public ValidatableResponse getDeviceById(DeviceId deviceId, int statusCode) {
        return given().spec(requestSpec)
                .pathParams("deviceId", deviceId.getId())
                .get("/api/device/{deviceId}")
                .then()
                .statusCode(statusCode);
    }

    public Device getDeviceById(DeviceId deviceId) {
        return getDeviceById(deviceId, HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public PageData<Device> getDevices(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/tenant/devices")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<Device>>() {
                });
    }

    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        return given().spec(requestSpec).get("/api/device/{deviceId}/credentials", deviceId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceCredentials.class);
    }

    public ValidatableResponse postTelemetry(String credentialsId, JsonNode telemetry) {
        return given().spec(requestSpec).body(telemetry)
                .post("/api/v1/{credentialsId}/telemetry", credentialsId)
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse deleteDevice(DeviceId deviceId) {
        return given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse deleteDeviceIfExists(DeviceId deviceId) {
        return given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }

    public ValidatableResponse deleteCalculatedFieldIfExists(CalculatedFieldId calculatedFieldId) {
        return given().spec(requestSpec)
                .delete("/api/calculatedField/{calculatedFieldId}", calculatedFieldId.getId())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }

    public ValidatableResponse postTelemetryAttribute(EntityId entityId, AttributeScope scope, JsonNode attribute) {
        return given().spec(requestSpec).body(attribute)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}", entityId.getEntityType(), entityId.getId(), scope.name())
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse postAttribute(String accessToken, JsonNode attribute) {
        return given().spec(requestSpec).body(attribute)
                .post("/api/v1/{accessToken}/attributes", accessToken)
                .then()
                .statusCode(HTTP_OK);
    }

    public JsonNode getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        return given().spec(requestSpec)
                .queryParam("clientKeys", clientKeys)
                .queryParam("sharedKeys", sharedKeys)
                .get("/api/v1/{accessToken}/attributes", accessToken)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    public ArrayNode getAttributes(EntityId entityId, AttributeScope scope, String keys) {
        return given().spec(requestSpec)
                .get("/api/plugins/telemetry/{entityType}/{entityId}/values/attributes/{scope}?keys={keys}", entityId.getEntityType(), entityId.getId(), scope, keys)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(ArrayNode.class);
    }


    public ValidatableResponse deleteEntityAttributes(EntityId entityId, AttributeScope scope, String keys) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("entityId", entityId.getId().toString());
        pathParams.put("entityType", entityId.getEntityType().name());
        pathParams.put("scope", scope.name());
        return given().spec(requestSpec)
                .pathParams(pathParams)
                .queryParam("keys", keys)
                .delete("/api/plugins/telemetry/{entityType}/{entityId}/{scope}")
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse deleteEntityTimeseries(EntityId entityId, String keys, boolean deleteAllDataForKeys) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("entityType", entityId.getEntityType().name());
        pathParams.put("entityId", entityId.getId().toString());
        return given().spec(requestSpec)
                .pathParams(pathParams)
                .queryParam("keys", keys)
                .queryParam("deleteAllDataForKeys", Boolean.toString(deleteAllDataForKeys))
                .delete("/api/plugins/telemetry/{entityType}/{entityId}/timeseries/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public JsonNode getLatestTelemetry(EntityId entityId) {
        return given().spec(requestSpec)
                .get("/api/plugins/telemetry/" + entityId.getEntityType().name() + "/" + entityId.getId() + "/values/timeseries")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    public JsonPath postProvisionRequest(String provisionRequest) {
        return given().spec(requestSpec)
                .body(provisionRequest)
                .post("/api/v1/provision")
                .getBody()
                .jsonPath();
    }

    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/ruleChains")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<RuleChain>>() {
                });
    }

    public RuleChain postRuleChain(RuleChain ruleChain) {
        return given().spec(requestSpec)
                .body(ruleChain)
                .post("/api/ruleChain")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChain.class);
    }

    public RuleChainMetaData postRuleChainMetadata(RuleChainMetaData ruleChainMetaData) {
        return given().spec(requestSpec)
                .body(ruleChainMetaData)
                .post("/api/ruleChain/metadata")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChainMetaData.class);
    }

    public void setRootRuleChain(RuleChainId ruleChainId) {
        given().spec(requestSpec)
                .post("/api/ruleChain/{ruleChainId}/root", ruleChainId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteRuleChain(RuleChainId ruleChainId) {
        given().spec(requestSpec)
                .delete("/api/ruleChain/{ruleChainId}", ruleChainId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    private String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    private void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

    public List<EntityRelation> findRelationByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return given().spec(requestSpec)
                .pathParams(params)
                .get("/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<EntityRelation>>() {
                });
    }

    public EntityRelation postEntityRelation(EntityRelation entityRelation) {
        return given().spec(requestSpec)
                .body(entityRelation)
                .post("/api/v2/relation")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityRelation.class);
    }


    public EntityRelation deleteEntityRelation(EntityId fromId, String relationType, EntityId toId) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fromId", fromId.getId().toString());
        queryParams.put("fromType", fromId.getEntityType().name());
        queryParams.put("relationType", relationType);
        queryParams.put("toId", toId.getId().toString());
        queryParams.put("toType", toId.getEntityType().name());
        return given().spec(requestSpec)
                .queryParams(queryParams)
                .delete("/api/v2/relation")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityRelation.class);
    }

    public JsonNode postServerSideRpc(DeviceId deviceId, JsonNode serverRpcPayload) {
        return given().spec(requestSpec)
                .body(serverRpcPayload)
                .post("/api/rpc/twoway/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    public Rpc getPersistedRpc(RpcId rpcId) {
        return given().spec(requestSpec)
                .get("/api/rpc/persistent/{rpcId}", rpcId.toString())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Rpc.class);
    }

    public PageData<Rpc> getPersistedRpcByDevice(DeviceId deviceId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/rpc/persistent/device/{deviceId}", deviceId.toString())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<>() {
                });
    }

    public PageData<DeviceProfile> getDeviceProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/deviceProfiles")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<DeviceProfile>>() {
                });
    }

    public DeviceProfile getDeviceProfileById(DeviceProfileId deviceProfileId) {
        return given().spec(requestSpec).get("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceProfile.class);
    }

    public DeviceProfile postDeviceProfile(DeviceProfile deviceProfile) {
        return given().spec(requestSpec).body(deviceProfile)
                .post("/api/deviceProfile")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceProfile.class);
    }

    public void deleteDeviceProfile(DeviceProfileId deviceProfileId) {
        given().spec(requestSpec)
                .delete("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public void setDefaultDeviceProfile(DeviceProfileId deviceProfileId) {
        given().spec(requestSpec)
                .post("/api/deviceProfile/{deviceProfileId}/default", deviceProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public AssetProfile postAssetProfile(AssetProfile assetProfile) {
        return given().spec(requestSpec).body(assetProfile)
                .post("/api/assetProfile")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(AssetProfile.class);
    }

    public PageData<AssetProfile> getAssetProfiles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/assetProfiles")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<AssetProfile>>() {
                });
    }

    public void deleteAssetProfile(AssetProfileId assetProfileId) {
        given().spec(requestSpec)
                .delete("/api/assetProfile/{assetProfileId}", assetProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public void setDefaultAssetProfile(AssetProfileId assetProfileId) {
        given().spec(requestSpec)
                .post("/api/assetProfile/{assetProfileId}/default", assetProfileId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public Customer postCustomer(Customer customer) {
        return given().spec(requestSpec)
                .body(customer)
                .post("/api/customer")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Customer.class);
    }

    public void deleteCustomer(CustomerId customerId) {
        given().spec(requestSpec)
                .delete("/api/customer/{customerId}", customerId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public PageData<Customer> getCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/customers")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<Customer>>() {
                });
    }

    public Alarm postAlarm(Alarm alarm) {
        return given().spec(requestSpec)
                .body(alarm)
                .post("/api/alarm")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Alarm.class);
    }

    public void deleteAlarm(AlarmId alarmId) {
        given().spec(requestSpec)
                .delete("/api/alarm/{alarmId}", alarmId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public User postUser(User user) {
        return given().spec(requestSpec)
                .body(user)
                .post("/api/user?sendActivationMail=false")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(User.class);
    }

    public UserId createUserAndLogin(User user, String password) {
        UserId userId = postUser(user).getId();
        getAndSetUserToken(userId);
        return userId;
    }

    public void getAndSetUserToken(UserId id) {
        ObjectNode tokenInfo = given().spec(requestSpec)
                .get("/api/user/" + id.getId().toString() + "/token")
                .then()
                .extract()
                .as(ObjectNode.class);
        token = tokenInfo.get("token").asText();
        refreshToken = tokenInfo.get("refreshToken").asText();
        requestSpec.header(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
    }

    protected void resetTokens() {
        this.token = null;
        this.refreshToken = null;
    }

    public void deleteUser(UserId userId) {
        given().spec(requestSpec)
                .delete("/api/user/{userId}", userId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public Asset postAsset(Asset asset) {
        return given().spec(requestSpec)
                .body(asset)
                .post("/api/asset")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Asset.class);
    }

    public Asset getAssetById(AssetId assetId) {
        return given().spec(requestSpec)
                .get("/api/asset/{assetId}", assetId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Asset.class);
    }

    public void deleteAsset(AssetId assetId) {
        given().spec(requestSpec)
                .delete("/api/asset/{assetId}", assetId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public EntityView postEntityView(EntityView entityView) {
        return given().spec(requestSpec)
                .body(entityView)
                .post("/api/entityView")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityView.class);
    }

    public EntityView getEntityViewById(EntityViewId entityViewId) {
        return given().spec(requestSpec)
                .get("/api/entityView/{entityViewId}", entityViewId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityView.class);
    }

    public void deleteEntityView(EntityViewId entityViewId) {
        given().spec(requestSpec)
                .delete("/api/entityView/{entityViewId}", entityViewId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public Dashboard postDashboard(Dashboard dashboard) {
        return given().spec(requestSpec)
                .body(dashboard)
                .post("/api/dashboard")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Dashboard.class);
    }

    public void deleteDashboard(DashboardId dashboardId) {
        given().spec(requestSpec)
                .delete("/api/dashboard/{dashboardId}", dashboardId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public void setDevicePublic(DeviceId deviceId) {
        given().spec(requestSpec)
                .post("/api/customer/public/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public PageData<EventInfo> getEvents(EntityId entityId, EventType eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType.name());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return given().spec(requestSpec)
                .get("/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&" + getTimeUrlParams(pageLink), params)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<>() {});
    }

    public ValidatableResponse postTbResourceIfNotExists(TbResource lwModel) {
        return given().spec(requestSpec).body(lwModel)
                .post("/api/resource")
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_BAD_REQUEST)));
    }

    public void deleteDeviceProfileIfExists(DeviceProfile deviceProfile) {
        given().spec(requestSpec)
                .delete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }

    public Device getDeviceByNameIfExists(String deviceName) {
        ValidatableResponse response = given().spec(requestSpec)
                .pathParams("deviceName", deviceName)
                .get("/api/tenant/devices?deviceName={deviceName}")
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
        if (((ValidatableResponseImpl) response).extract().response().getStatusCode() == HTTP_OK) {
            return response.extract()
                    .as(Device.class);
        } else {
            return null;
        }
    }

    public DeviceCredentials postDeviceCredentials(DeviceCredentials deviceCredentials) {
        return given().spec(requestSpec).body(deviceCredentials)
                .post("/api/device/credentials")
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(DeviceCredentials.class);
    }

    private void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        this.addPageLinkToParam(params, pageLink);
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    private String getTimeUrlParams(TimePageLink pageLink) {
        String urlParams = getUrlParams(pageLink);
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }

    public PageData<EntityData> postEntityDataQuery(EntityDataQuery entityDataQuery) {
        return given().spec(requestSpec).body(entityDataQuery)
                .post("/api/entitiesQuery/find")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<>() {});
    }

    public Long postCountDataQuery(EntityCountQuery entityCountQuery) {
        return given().spec(requestSpec).body(entityCountQuery)
                .post("/api/entitiesQuery/count")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Long.class);
    }

    public EdqsState getEdqsState() {
        return given().spec(requestSpec)
                .get("/api/edqs/state")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EdqsState.class);
    }

    public void assignDeviceToCustomer(CustomerId customerId, DeviceId id) {
        given().spec(requestSpec)
                .post("/api/customer/" + customerId.getId().toString() + "/device/" + id.getId().toString())
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteTenant(TenantId tenantId) {
        given().spec(requestSpec)
                .delete("/api/tenant/" + tenantId.getId().toString())
                .then()
                .statusCode(HTTP_OK);
    }

    public TenantProfile postTenantProfile(TenantProfile tenantProfile) {
        return given().spec(requestSpec).body(tenantProfile)
                .post("/api/tenantProfile")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(TenantProfile.class);
    }

    public EntityInfo getDefaultTenantProfileInfo() {
        return given().spec(requestSpec)
                .get("/api/tenantProfileInfo/default")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityInfo.class);
    }

    public TenantProfile getTenantProfileById(String tenantProfileId) {
        return given().spec(requestSpec)
                .pathParams("tenantProfileId", tenantProfileId)
                .get("/api/tenantProfile/{tenantProfileId}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(TenantProfile.class);
    }

}
