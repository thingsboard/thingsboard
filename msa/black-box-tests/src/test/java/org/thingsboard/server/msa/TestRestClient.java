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
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.springframework.http.HttpStatus;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.thingsboard.server.common.data.StringUtils.isEmpty;
import static org.thingsboard.server.msa.AbstractContainerTest.getRequestFactoryForSelfSignedCert;

public class TestRestClient {
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    private final String baseURL;
    private String token;
    private String refreshToken;
    private RequestSpecification spec;
    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";

    public TestRestClient(String url) {
        baseURL = url;
        spec = given().baseUri(baseURL).contentType(ContentType.JSON);
        if (url.matches("^(https)://.*$")) {
            spec.relaxedHTTPSValidation();
        }
    }

    public void login(String username, String password) throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        JsonPath jsonPath = given().relaxedHTTPSValidation().body(loginRequest).post(baseURL + "/api/auth/login")
                .getBody().jsonPath();
        token = jsonPath.get("token");
        refreshToken = jsonPath.get("refreshToken");
        spec.header(JWT_TOKEN_HEADER_PARAM, "Bearer " + token)
                .contentType(ContentType.JSON);
    }

    public Device postDevice(String accessToken, Device device) {
        return  given().spec(spec).body(device)
                .pathParams("accessToken", accessToken)
                .post("/api/device?accessToken={accessToken}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(Device.class);
    }

    public ValidatableResponse getDeviceById(DeviceId deviceId, int statusCode) {
        return  given().spec(spec)
                .pathParams("deviceId", deviceId.getId())
                .get("/api/device/{deviceId}")
                .then()
                .statusCode(statusCode);
    }
    public Device getDeviceById(DeviceId deviceId) {
        return  getDeviceById(deviceId, HttpStatus.OK.value())
                .extract()
                .as(Device.class);
    }
    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        return given().spec(spec).get("/api/device/{deviceId}/credentials", deviceId.getId())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .as(DeviceCredentials.class);
    }

    public ValidatableResponse postTelemetry(String credentialsId, JsonNode telemetry) {
         return  given().spec(spec).body(telemetry)
                 .post("/api/v1/{credentialsId}/telemetry", credentialsId)
                 .then()
                 .statusCode(HttpStatus.OK.value());
    }

    public ValidatableResponse deleteDevice(DeviceId deviceId) {
        return  given().spec(spec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HttpStatus.OK.value());
    }
    public ValidatableResponse deleteDeviceIfExists(DeviceId deviceId) {
        return  given().spec(spec)
                .delete("/api/device/{deviceId}", deviceId.getId())
                .then()
                .statusCode(anyOf(is(HttpStatus.OK.value()),is(HttpStatus.NOT_FOUND.value())));
    }

    public ValidatableResponse postTelemetryAttribute(String entityType, DeviceId deviceId, String scope, JsonNode attribute) {
        return  given().spec(spec).body(attribute)
                .post("/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}", entityType, deviceId.getId(), scope)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    public ValidatableResponse postAttribute(String accessToken, JsonNode attribute) {
        return  given().spec(spec).body(attribute)
                .post("/api/v1/{accessToken}/attributes/", accessToken)
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    public JsonNode getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        return  given().spec(spec)
                .queryParam("clientKeys", clientKeys)
                .queryParam("sharedKeys", sharedKeys)
                .get("/api/v1/{accessToken}/attributes", accessToken)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(JsonNode.class);
    }

    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return given().spec(spec).queryParams(params)
                .get("/api/ruleChains")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(new TypeRef<PageData<RuleChain>>() {});
    }

    public RuleChain postRootRuleChain(RuleChain ruleChain) {
        return given().spec(spec)
                .body(ruleChain)
                .post("/api/ruleChain")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(RuleChain.class);
    }

    public RuleChainMetaData postRuleChainMetadata(RuleChainMetaData ruleChainMetaData) {
        return given().spec(spec)
                .body(ruleChainMetaData)
                .post("/api/ruleChain/metadata")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(RuleChainMetaData.class);
    }

    public void setRootRuleChain(RuleChainId ruleChainId) {
        given().spec(spec)
                .post("/api/ruleChain/{ruleChainId}/root", ruleChainId.getId())
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    public void deleteRuleChain(RuleChainId ruleChainId) {
        given().spec(spec)
                .delete("/api/ruleChain/{ruleChainId}", ruleChainId.getId())
                .then()
                .statusCode(HttpStatus.OK.value());
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

        return given().spec(spec)
                .pathParams(params)
                .get("/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(new TypeRef<List<EntityRelation>>() {});
    }

    public JsonNode postServerSideRpc(DeviceId deviceId, JsonNode serverRpcPayload) {
        return given().spec(spec)
                .body(serverRpcPayload)
                .post("/api/rpc/twoway/{deviceId}", deviceId.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(JsonNode.class);
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
