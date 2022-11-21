/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.config.HeaderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
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
                .post( "/api/auth/login")
                .getBody().jsonPath();
        token = jsonPath.get("token");
        refreshToken = jsonPath.get("refreshToken");
        requestSpec.header(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
    }

    public Device postDevice(String accessToken, Device device) {
        return  given().spec(requestSpec).body(device)
                .pathParams("accessToken", accessToken)
                .post("/api/device?accessToken={accessToken}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public Device getDeviceByName(String deviceName) {
        return given().spec(requestSpec).pathParam("deviceName", deviceName)
                .get("/api/tenant/devices{deviceName}")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(Device.class);
    }

    public ValidatableResponse getDeviceById(DeviceId deviceId, int statusCode) {
        return  given().spec(requestSpec)
                .pathParams("deviceId", deviceId.getId())
                .get("/api/device/{deviceId}")
                .then()
                .statusCode(statusCode);
    }
    public Device getDeviceById(DeviceId deviceId) {
        return  getDeviceById(deviceId, HTTP_OK)
                .extract()
                .as(Device.class);
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
         return  given().spec(requestSpec).body(telemetry)
                 .post("/api/v1/{credentialsId}/telemetry", credentialsId)
                 .then()
                 .statusCode(HTTP_OK);
    }

    public ValidatableResponse deleteDevice(DeviceId deviceId) {
        return  given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HTTP_OK);
    }
    public ValidatableResponse deleteDeviceIfExists(DeviceId deviceId) {
        return  given().spec(requestSpec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(anyOf(is(HTTP_OK),is(HTTP_NOT_FOUND)));
    }

    public ValidatableResponse postTelemetryAttribute(String entityType, DeviceId deviceId, String scope, JsonNode attribute) {
        return  given().spec(requestSpec).body(attribute)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}", entityType, deviceId.getId(), scope)
                .then()
                .statusCode(HTTP_OK);
    }

    public ValidatableResponse postAttribute(String accessToken, JsonNode attribute) {
        return  given().spec(requestSpec).body(attribute)
                .post("/api/v1/{accessToken}/attributes/", accessToken)
                .then()
                .statusCode(HTTP_OK);
    }

    public JsonNode getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        return  given().spec(requestSpec)
                .queryParam("clientKeys", clientKeys)
                .queryParam("sharedKeys", sharedKeys)
                .get("/api/v1/{accessToken}/attributes", accessToken)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(JsonNode.class);
    }

    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(requestSpec).queryParams(params)
                .get("/api/ruleChains")
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .as(new TypeRef<PageData<RuleChain>>() {});
    }

    public RuleChain postRootRuleChain(RuleChain ruleChain) {
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
                .as(new TypeRef<List<EntityRelation>>() {});
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

    public DeviceProfile getDeviceProfileById(DeviceProfileId deviceProfileId) {
        return  given().spec(requestSpec).get("/api/deviceProfile/{deviceProfileId}", deviceProfileId.getId())
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

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
