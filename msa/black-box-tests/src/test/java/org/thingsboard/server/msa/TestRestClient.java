// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rest.client.utils.RestJsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
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
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportRequest;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.setup.SetupInfo;
import org.thingsboard.server.common.data.setup.SystemSetupRequest;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.trendz.TrendzHealthcheckResult;
import org.thingsboard.server.common.data.trendz.TrendzSummary;
import org.thingsboard.server.common.data.trendz.TrendzSynchronizationResult;

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

@Slf4j
public class TestRestClient {

    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final int SETUP_STATE_MAX_ATTEMPTS = 60;
    private static final long SETUP_STATE_RETRY_DELAY_MS = 2000;
    /** The demo tenant administrator the demo data creates, and the account most suites sign in as. */
    private static final String DEMO_TENANT_ADMIN_EMAIL = "tenant@thingsboard.org";
    private static final String DEMO_TENANT_ADMIN_PASSWORD = "tenant";
    private final String baseUrl;
    private final RequestSpecification requestSpec;
    private String token;
    private String refreshToken;

    public TestRestClient(String url) {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());

        baseUrl = url;
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

    public SetupInfo getSystemSetupState() {
        return given().spec(requestSpec)
                .get("/api/noauth/setup/state")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(SetupInfo.class);
    }

    public void completeSystemSetup(String email, String password, boolean loadDemo) {
        SystemSetupRequest systemSetupRequest = new SystemSetupRequest();
        systemSetupRequest.setEmail(email);
        systemSetupRequest.setPassword(password);
        systemSetupRequest.setLoadDemo(loadDemo);
        given().spec(requestSpec).body(systemSetupRequest)
                .post("/api/noauth/setup/complete")
                .then()
                .statusCode(HTTP_OK);
    }

    /**
     * Runs the no-auth first-time setup flow once at suite bring-up. Waits for the license to activate
     * (state leaves {@link SystemSetupState#LICENSE_REQUIRED}), then, if a sysadmin is still missing,
     * creates the default sysadmin and (with {@code loadDemo}) the demo tenant/customers/devices, flipping
     * the platform to {@link SystemSetupState#READY}. Idempotent: if the platform is already READY it does
     * nothing, so re-running against a provisioned instance is safe.
     */
    public void ensureSystemSetup(String sysAdminEmail, String sysAdminPassword, boolean loadDemo) {
        SystemSetupState state = null;
        for (int attempt = 1; attempt <= SETUP_STATE_MAX_ATTEMPTS; attempt++) {
            state = getSystemSetupState().getStatus();
            if (state != SystemSetupState.LICENSE_REQUIRED) {
                break;
            }
            log.info("Waiting for license activation before first-time setup, current state {} (attempt {}/{})",
                    state, attempt, SETUP_STATE_MAX_ATTEMPTS);
            try {
                Thread.sleep(SETUP_STATE_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for license activation", e);
            }
        }
        if (state == SystemSetupState.READY) {
            log.info("System is already set up, skipping first-time setup");
            return;
        }
        if (state == SystemSetupState.LICENSE_REQUIRED) {
            // Distinct from the generic message below because it is the failure a CI reader will actually hit,
            // and it points at a different cause: the platform never activated within the wait budget. The
            // suite runs keyless, so that means NON_PRODUCTION_USE did not reach the node, or the keyless
            // allowance is spent - not that the platform is in an odd state.
            throw new IllegalStateException("License was not activated after waiting "
                    + (SETUP_STATE_MAX_ATTEMPTS * SETUP_STATE_RETRY_DELAY_MS / 1000) + " seconds ("
                    + SETUP_STATE_MAX_ATTEMPTS + " attempts every " + SETUP_STATE_RETRY_DELAY_MS
                    + " ms); check that NON_PRODUCTION_USE=true reached tb-node.env and that no license secret is set");
        }
        if (state != SystemSetupState.ACCOUNT_REQUIRED) {
            throw new IllegalStateException("System is not ready for first-time setup, current state: " + state);
        }
        log.info("Creating default sysadmin '{}' and loading demo data ({})", sysAdminEmail, loadDemo);
        completeSystemSetup(sysAdminEmail, sysAdminPassword, loadDemo);
        if (loadDemo) {
            verifyDemoTenantAdminCanSignIn();
        }
    }

    /**
     * The demo data load is best-effort on the server: it is caught and the setup still answers 200. Since it is
     * the only source of the demo tenant administrator every suite signs in as, a swallowed failure would
     * otherwise surface much later as a wall of unexplained 401s. Checked here so it is reported where it happened.
     * <p>
     * On a throwaway client: {@link #login} replaces this one's token, refresh token and Authorization header,
     * and the suite's shared client must stay signed out at the end of bring-up.
     */
    private void verifyDemoTenantAdminCanSignIn() {
        TestRestClient demoTenantAdminClient = new TestRestClient(baseUrl);
        demoTenantAdminClient.login(DEMO_TENANT_ADMIN_EMAIL, DEMO_TENANT_ADMIN_PASSWORD);
        if (isEmpty(demoTenantAdminClient.getToken())) {
            throw new IllegalStateException("The demo tenant administrator " + DEMO_TENANT_ADMIN_EMAIL +
                    " cannot sign in after the first-time setup: loading the demo data failed, which the setup " +
                    "endpoint reports as success. Check the tb-node logs for the demo data load.");
        }
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

    public void reprocessCalculatedField(CalculatedField calculatedField, long startTs, long endTs) {
        given().spec(requestSpec)
                .get("/api/calculatedField/" + calculatedField.getUuidId() + "/reprocess?startTs={startTs}&endTs={endTs}", startTs, endTs)
                .then()
                .statusCode(HTTP_OK);
    }

    public void reprocessCalculatedFieldAndWait(CalculatedField calculatedField, long startTs, long endTs) {
        given().spec(requestSpec)
                .get("/api/calculatedField/" + calculatedField.getUuidId() + "/reprocessAndWait?startTs={startTs}&endTs={endTs}", startTs, endTs)
                .then()
                .statusCode(HTTP_OK);
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

    public ValidatableResponse postTelemetry(EntityId entityId, JsonNode telemetry) {
        return given().spec(requestSpec).body(telemetry)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/timeseries/SERVER_SCOPE", entityId.getEntityType(), entityId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public List<JsonNode> getEntityAttributeByScopeAndKey(EntityId entityId, String scope, String key) {
        return given().spec(requestSpec)
                .get("/api/plugins/telemetry/{entityType}/{entityId}/values/attributes/{scope}?keys={key}", entityId.getEntityType(), entityId.getId(), scope, key)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<JsonNode>>() {
                });
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

    public ObjectNode getTimeSeries(EntityId entityId, long startTs, long endTs, String... keys) {
        return given().spec(requestSpec)
                .get("/api/plugins/telemetry/" + entityId.getEntityType().name() + "/" + entityId.getId() + "/values/timeseries?keys={keys}&startTs={startTs}&endTs={endTs}", String.join(",", keys), startTs, endTs)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(ObjectNode.class);
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

    public JsonNode testRuleChainScript(Object body) {
        return given().spec(requestSpec)
                .body(body)
                .post("/api/ruleChain/testScript")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
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

    public void deleteDeviceProfileIfExists(DeviceProfile deviceProfile) {
        given().spec(requestSpec)
                .delete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }


    public RuleChainMetaData getRuleChainMetadata(RuleChainId ruleChainId) {
        return given().spec(requestSpec)
                .get("/api/ruleChain/{ruleChainId}/metadata", ruleChainId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChainMetaData.class);
    }

    public Converter postConverter(Converter converter) {
        return given().spec(requestSpec)
                .body(converter)
                .post("/api/converter")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Converter.class);
    }

    public void deleteIntegration(IntegrationId integrationId) {
        given().spec(requestSpec)
                .delete("/api/integration/{integrationId}", integrationId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteConverter(ConverterId converterId) {
        given().spec(requestSpec)
                .delete("/api/converter/{converterId}", converterId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse postUplinkPayloadForHttpIntegration(String integrationKey, JsonNode jsonNode) {
        return postUplinkPayloadForHttpBasedIntegration(integrationKey, jsonNode, IntegrationType.HTTP);
    }

    public ValidatableResponse postUplinkPayloadForHttpBasedIntegration(String integrationKey, JsonNode jsonNode, IntegrationType integrationType) {
        return given().spec(requestSpec)
                .body(jsonNode)
                .post("/api/v1/integrations/" + integrationType.name().toLowerCase() + "/" + integrationKey)
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse postUplinkPayloadForHttpIntegration(String integrationKey, JsonNode jsonNode, Map<String, Object> headers) {
        return postUplinkPayloadForHttpBasedIntegration(integrationKey, jsonNode, IntegrationType.HTTP, headers);
    }

    public ValidatableResponse postUplinkPayloadForHttpBasedIntegration(String integrationKey, JsonNode jsonNode, IntegrationType integrationType, Map<String, Object> headers) {
        return given().spec(requestSpec)
                .headers(headers)
                .body(jsonNode)
                .post("/api/v1/integrations/" + integrationType.name().toLowerCase() + "/" + integrationKey)
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse postUplinkPayloadForHttpBasedIntegrationForExpectedErrorStatusCode(String integrationKey, JsonNode jsonNode, Map<String, Object> headers, IntegrationType integrationType, int statusCode) {
        RequestSpecification spec = given().spec(requestSpec);
        if (headers != null) {
            spec.headers(headers);
        }
        return spec.body(jsonNode)
                .post("/api/v1/integrations/" + integrationType.name().toLowerCase() + "/" + integrationKey)
                .then()
                .statusCode(statusCode);
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

    public Integration postIntegration(Integration integration) {
        return given().spec(requestSpec)
                .body(integration)
                .post("/api/integration")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Integration.class);
    }

    public void checkConnection(Integration integration) {
        given().spec(requestSpec)
                .body(integration)
                .post("/api/integration/check")
                .then()
                .statusCode(HTTP_OK);
    }

    public void saveEntityAttributes(String entityType, String entityId, String scope, JsonNode request) {
        given().spec(requestSpec)
                .body(request)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}", entityType, entityId, scope)
                .then()
                .statusCode(HTTP_OK);
    }

    public RuleChain saveRuleChain(RuleChain ruleChain) {
        return given().spec(requestSpec)
                .body(ruleChain)
                .post("/api/ruleChain")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(RuleChain.class);
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys) {
        return getLatestTimeseries(entityId, keys, true);
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys, boolean useStrictDataTypes) {
        Map<String, List<JsonNode>> timeseries = given().spec(requestSpec)
                .get("/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&useStrictDataTypes={useStrictDataTypes}",
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        String.join(",", keys),
                        useStrictDataTypes)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<Map<String, List<JsonNode>>>() {
                });

        return RestJsonConverter.toTimeseries(timeseries);
    }

    public List<String> getTimeseriesKeys(EntityId entityId) {
        return given().spec(requestSpec)
                .get("/api/plugins/telemetry/{entityType}/{entityId}/keys/timeseries",
                        entityId.getEntityType().name(),
                        entityId.getId().toString())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<String>>() {
                });
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

    public Customer postCustomer(Customer customer, EntityGroupId id) {
        return given().spec(requestSpec)
                .body(customer)
                .post("/api/customer?entityGroupId={id}", id.getId())
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

    public List<EntityGroupInfo> getEntityGroups(EntityType groupType) {
        return given().spec(requestSpec)
                .get("/api/entityGroups/{groupType}", groupType)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<EntityGroupInfo>>() {
                });
    }

    public EntityGroupInfo postEntityGroup(EntityGroup entityGroup) {
        return given().spec(requestSpec)
                .body(entityGroup)
                .post("/api/entityGroup")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(EntityGroupInfo.class);
    }

    public List<EntityGroupInfo> getEntityGroupsByOwnerAndType(EntityType ownerType, EntityId ownerId, EntityType groupType) {
        return given().spec(requestSpec)
                .get("/api/entityGroups/{ownerType}/{ownerId}/{groupType}", ownerType, ownerId.getId(), groupType)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<EntityGroupInfo>>() {
                });
    }

    public void deleteEntityGroup(EntityGroupId entityGroupId) {
        given().spec(requestSpec)
                .delete("/api/entityGroup/{entityGroupId}", entityGroupId.getId())
                .then()
                .statusCode(HTTP_OK);
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

    public User postUser(User user, EntityGroupId entityId) {
        return given().spec(requestSpec)
                .body(user)
                .post("/api/user?sendActivationMail=false&entityGroupIds={entityId}", entityId.getId())
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

    public void deleteTenant(TenantId tenantId) {
        given().spec(requestSpec)
                .delete("/api/tenant/" + tenantId.getId().toString())
                .then()
                .statusCode(HTTP_OK);
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void postTemperatureHumidity() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/temperature_sensors/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteTemperatureHumidity() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/temperature_sensors/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postSmartOffice() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/smart_office/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteSmartOffice() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/smart_office/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postFleetTracking() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/fleet_tracking/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteFleetTracking() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/fleet_tracking/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postWaterMetering() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/water_metering/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteWaterMetering() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/water_metering/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postAirQualityMonitoring() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/air_quality_index/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteAirQualityMonitoring() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/air_quality_index/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postSmartRetail() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/smart_retail/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteSmartRetail() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/smart_retail/delete")
                .then()
                .statusCode(HTTP_OK);
    }

    public void postSmartIrrigation() {
        given().spec(requestSpec)
                .post("/api/solutions/templates/smart_irrigation/install")
                .then()
                .statusCode(HTTP_OK);
    }

    public void deleteSmartIrrigation() {
        given().spec(requestSpec)
                .delete("/api/solutions/templates/smart_irrigation/delete")
                .then()
                .statusCode(HTTP_OK);
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

    public PageData<Asset> getTenantAssets(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/tenant/assets")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<Asset>>() {
                });
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

    public List<DashboardInfo> getDashboardsByEntityGroupId(PageLink pageLink, EntityGroupId entityGroupId) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/entityGroup/{entityGroupId}/dashboards", entityGroupId.getId())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<DashboardInfo>>() {
                }).getData();
    }

    public void setEntityGroupPublic(EntityGroupId entityGroupId) {
        given().spec(requestSpec)
                .post("/api/entityGroup/{entityGroupId}/makePublic", entityGroupId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public Device postDeviceWithCredentials(String accessToken, Device device) {
        return given().spec(requestSpec).body(device)
                .pathParams("accessToken", accessToken)
                .post("/api/device?accessToken={accessToken}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public Device getDeviceByNameIfExists(String deviceName) {
        ValidatableResponse response = given().spec(requestSpec)
                .pathParams("deviceName", deviceName)
                .get("/api/tenant/devices?deviceName={deviceName}")
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
        if (((ValidatableResponseImpl) response).extract().response().getStatusCode() == HTTP_OK) {
            return response.extract().as(Device.class);
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

    public ValidatableResponse postTbResourceIfNotExists(TbResource lwModel) {
        return given().spec(requestSpec).body(lwModel)
                .post("/api/resource")
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_BAD_REQUEST)));
    }

    public PageData<TbResource> getTbResources(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/resource")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<TbResource>>() {
                });
    }

    public void deleteDeviceProfileIfExists(DeviceProfileId deviceProfileId) {
        given().spec(requestSpec)
                .delete("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
                .then()
                .statusCode(anyOf(is(HTTP_OK), is(HTTP_NOT_FOUND)));
    }

    public EntityGroupInfo findCustomerAdminsGroup(CustomerId customerId) throws Exception {
        return findGroupByOwnerIdTypeAndName(customerId, EntityType.USER, EntityGroup.GROUP_CUSTOMER_ADMINS_NAME);
    }

    public EntityGroupInfo findGroupByOwnerIdTypeAndName(EntityId ownerId, EntityType groupType, String name) throws Exception {
        List<EntityGroupInfo> groupsList = getEntityGroupsByOwnerAndType(ownerId, groupType);
        EntityGroupInfo result = null;
        for (EntityGroupInfo tmp : groupsList) {
            if (name.equals(tmp.getName())) {
                result = tmp;
            }
        }
        return result;
    }

    public List<EntityGroupInfo> getEntityGroupsByOwnerAndType(EntityId ownerId, EntityType groupType) throws Exception {
        return given().spec(requestSpec)
                .get("/api/entityGroups/" + ownerId.getEntityType() + "/" + ownerId.getId() + "/" + groupType.name())
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<List<EntityGroupInfo>>() {
                });
    }

    public ReportTemplate postReportTemplate(ReportTemplate reportTemplate) {
        return given().spec(requestSpec)
                .body(reportTemplate)
                .post("/api/reportTemplate")
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(ReportTemplate.class);
    }

    public byte[] requestTestReport(ReportRequest request) {
        return given().spec(requestSpec)
                .body(request)
                .accept("text/csv")
                .post("/api/v2/report/test")
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .asByteArray();
    }

    public Job requestReport(ReportRequest request) {
        return given().spec(requestSpec)
                .body(request)
                .post("/api/v2/report/request")
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(Job.class);
    }

    public Job getJobById(JobId id) {
        return given().spec(requestSpec)
                .get("/api/job/{id}", id.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .as(Job.class);
    }

    public byte[] downloadBlobEntity(BlobEntityId blobEntityId) {
        return given().spec(requestSpec)
                .get("/api/blobEntity/{blobEntityId}/download", blobEntityId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .asByteArray();
    }

    public byte[] downloadReport(ReportId reportId) {
        return given().spec(requestSpec)
                .get("/api/v2/report/{reportId}/download", reportId.getId())
                .then()
                .assertThat()
                .statusCode(HTTP_OK)
                .extract()
                .asByteArray();
    }

    public void changeOwner(EntityId ownerId, EntityId entityId) {
        given().spec(requestSpec)
                .post("/api/owner/{ownerEntityType}/{ownerId}/{entityType}/{entityId}", ownerId.getEntityType().name(), ownerId.getId(), entityId.getEntityType().name(), entityId.getId())
                .then()
                .statusCode(HTTP_OK);
    }

    public Secret saveSecret(Secret secret) {
        return given().spec(requestSpec)
                .body(secret)
                .post("/api/secret")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Secret.class);
    }

    public ValidatableResponse deleteSecret(SecretId secretId) {
        return given().spec(requestSpec)
                .delete("/api/secret/{secretId}", secretId.getId())
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

    public TrendzSynchronizationResult connectTrendz() {
        return given().spec(requestSpec)
                .post("/api/trendz/connect")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(TrendzSynchronizationResult.class);
    }

    public void enableUserCredentials(UserId userId) {
        given().spec(requestSpec)
                .post("/api/user/" + userId.getId() + "/userCredentialsEnabled")
                .then()
                .statusCode(HTTP_OK);
    }

    public TrendzHealthcheckResult trendzHealthcheck() {
        return given().spec(requestSpec)
                .get("/api/trendz/healthcheck")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(TrendzHealthcheckResult.class);
    }

    public TrendzSummary getTrendzSummary() {
        return given().spec(requestSpec)
                .get("/api/trendz/summary")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(TrendzSummary.class);
    }
}
