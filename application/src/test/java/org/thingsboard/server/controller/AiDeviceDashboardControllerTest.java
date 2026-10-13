// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.ai.common.client.TbAiClient;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class AiDeviceDashboardControllerTest extends AbstractAiControllerTest {

    private static final Map<Resource, List<Operation>> AI_DASHBOARD_BASE_PERMISSIONS = Map.of(
            Resource.AI, List.of(Operation.WRITE),
            Resource.DEVICE, List.of(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY),
            Resource.DASHBOARD, List.of(Operation.CREATE)
    );

    @MockitoSpyBean
    private ApiLimitService apiLimitService;

    private Device device;

    @Before
    public void setup() throws Exception {
        loginTenantAdmin();

        Device d = new Device();
        d.setName("AI Dashboard Test Device");
        d.setType("default");
        device = doPost("/api/device", d, Device.class);
    }

    @Test
    public void shouldCallTbAiClientWithDeviceIdRequestAndUserToken_whenTenantAdmin() {
        // GIVEN
        ObjectNode request = JacksonUtil.newObjectNode();
        ObjectNode response = generatedDashboardResponse();
        givenAiClientGeneratesDashboard(request, response);

        // WHEN
        JsonNode result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", request, JsonNode.class);

        // THEN
        assertThat(result).isEqualTo(response);

        TbAiClient.TokenProvider tokenProvider = verifyAiClientGeneratedDashboard(request);
        assertTokenProviderBelongsToTenantAdmin(tokenProvider);
    }

    @Test
    public void shouldForwardTimeseriesKeys_whenRequestBodySpecifiesThem() throws Exception {
        // GIVEN
        JsonNode request = JacksonUtil.toJsonNode("{\"timeseriesKeys\":[\"temperature\",\"humidity\"]}");
        givenAiClientGeneratesDashboard(request, generatedDashboardResponse());

        // WHEN
        doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", request)
                .andExpect(status().isOk());

        // THEN
        verifyAiClientGeneratedDashboard(request);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallTbAiClient_whenUserIsSysAdmin() throws Exception {
        // GIVEN
        loginSysAdmin();

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnForbiddenAndNotCallTbAiClient_whenUserIsCustomerUser() throws Exception {
        // GIVEN
        loginCustomerAdminUser();

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermission)));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldCallTbAiClient_whenRestrictedTenantAdminHasAllRequiredPermissions() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS);
        ObjectNode request = JacksonUtil.newObjectNode();
        givenAiClientGeneratesDashboard(request, generatedDashboardResponse());

        // WHEN
        doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", request)
                .andExpect(status().isOk());

        // THEN
        verifyAiClientGeneratedDashboard(request);
    }

    @Test
    public void shouldDenyAndNotCallTbAiClient_whenUserIsMissingAiWritePermission() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS,
                Map.of(Resource.AI, List.of(Operation.WRITE)));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionWrite + AI_RESOURCE)));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldDenyAndNotCallTbAiClient_whenUserIsMissingDeviceReadPermission() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS,
                Map.of(Resource.DEVICE, List.of(Operation.READ)));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionRead + "DEVICE '" + device.getName() + "'!")));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldDenyAndNotCallTbAiClient_whenUserIsMissingDeviceReadAttributesPermission() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS,
                Map.of(Resource.DEVICE, List.of(Operation.READ_ATTRIBUTES)));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo("You don't have permission to perform 'READ_ATTRIBUTES' operation with DEVICE '" + device.getName() + "'!")));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldDenyAndNotCallTbAiClient_whenUserIsMissingDeviceReadTelemetryPermission() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS,
                Map.of(Resource.DEVICE, List.of(Operation.READ_TELEMETRY)));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo("You don't have permission to perform 'READ_TELEMETRY' operation with DEVICE '" + device.getName() + "'!")));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldDenyAndNotCallTbAiClient_whenUserIsMissingDashboardCreatePermission() throws Exception {
        // GIVEN
        loginAsRestrictedTenantAdmin(tenantId, AI_DASHBOARD_BASE_PERMISSIONS,
                Map.of(Resource.DASHBOARD, List.of(Operation.CREATE)));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(statusReason(equalTo(msgErrorPermissionCreate + "'DASHBOARD' resource!")));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldReturnLimitExceededAndNotCallTbAiClient_whenDashboardLimitIsReached() throws Exception {
        // GIVEN
        givenDashboardLimitReached(1);

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("Dashboards limit reached")))
                .andExpect(jsonPath("$.errorCode", equalTo(41)))
                .andExpect(jsonPath("$.entityType", equalTo("DASHBOARD")))
                .andExpect(jsonPath("$.limit", equalTo(1)));

        verifyNoInteractions(tbAiClient);
    }

    @Test
    public void shouldPropagateAiServiceErrorMessage_whenAiClientReturnsFailure() throws Exception {
        // GIVEN
        String aiServiceError = "Failed to save dashboard: dashboards limit reached";
        given(tbAiClient.generateDashboard(eq(device.getUuidId()), any(JsonNode.class), any(String.class), any(TbAiClient.TokenProvider.class)))
                .willReturn(failure(aiServiceError));

        // WHEN
        ResultActions result = doPost("/api/ai/devices/" + device.getUuidId() + "/dashboard", JacksonUtil.newObjectNode());

        // THEN
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", equalTo(aiServiceError)));

        verify(tbAiClient).generateDashboard(eq(device.getUuidId()), any(JsonNode.class), any(String.class), any(TbAiClient.TokenProvider.class));
    }

    private void givenDashboardLimitReached(long limit) {
        doReturn(false).when(apiLimitService).checkEntitiesLimit(tenantId, EntityType.DASHBOARD);
        doReturn(limit).when(apiLimitService).getLimit(eq(tenantId), any());
    }

    private void givenAiClientGeneratesDashboard(JsonNode request, JsonNode response) {
        given(tbAiClient.generateDashboard(eq(device.getUuidId()), eq(request), any(String.class), any(TbAiClient.TokenProvider.class)))
                .willReturn(success(response));
    }

    private TbAiClient.TokenProvider verifyAiClientGeneratedDashboard(JsonNode request) {
        ArgumentCaptor<TbAiClient.TokenProvider> tokenProviderCaptor = ArgumentCaptor.forClass(TbAiClient.TokenProvider.class);
        verify(tbAiClient).generateDashboard(eq(device.getUuidId()), eq(request), eq("Bearer " + token), tokenProviderCaptor.capture());
        verifyNoMoreInteractions(tbAiClient);
        return tokenProviderCaptor.getValue();
    }

    private static ObjectNode generatedDashboardResponse() {
        return JacksonUtil.newObjectNode()
                .put("dashboardId", UUID.randomUUID().toString());
    }

}
