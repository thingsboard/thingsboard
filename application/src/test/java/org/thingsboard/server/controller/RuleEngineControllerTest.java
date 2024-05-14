/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RuleEngineControllerTest extends AbstractControllerTest {

    private static final String REQUEST_BODY = "{\"temperature\":23}";

    @SpyBean
    private RuleEngineCallService ruleEngineCallService;

    @Autowired
    private JwtTokenFactory jwtTokenFactory;

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorUser() throws Exception {
        loginSysAdmin();
        UserId sysAdminUserId = getCurrentUserId();
        TbMsg msg = TbMsg.newMsg(TbMsgType.REST_API_REQUEST, sysAdminUserId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), TbMsgMetaData.EMPTY, REQUEST_BODY);
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(4);
            consumer.accept(msg);
            return null;
        }).when(ruleEngineCallService).processRestApiCallToRuleEngine(eq(TenantId.SYS_TENANT_ID), any(UUID.class), any(TbMsg.class), anyBoolean(), any());

        var response = doPostAsyncWithTypedResponse("/api/rule-engine/", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(Objects.requireNonNull(JacksonUtil.toString(response))).isEqualTo(REQUEST_BODY);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(TenantId.SYS_TENANT_ID), any(), captor.capture(), eq(false), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getOriginator()).isEqualTo(sysAdminUserId);
        testLogEntityAction(null, sysAdminUserId, TenantId.SYS_TENANT_ID, new CustomerId(TenantId.SYS_TENANT_ID.getId()), sysAdminUserId,
                SYS_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, REQUEST_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDevice() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg msg = TbMsg.newMsg(TbMsgType.REST_API_REQUEST, deviceId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), TbMsgMetaData.EMPTY, REQUEST_BODY);
        mockSuccessfulRestApiCallToRuleEngine(msg);

        var response = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId(), REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(Objects.requireNonNull(JacksonUtil.toString(response))).isEqualTo(REQUEST_BODY);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(), captor.capture(), eq(false), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getOriginator()).isEqualTo(deviceId);
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, REQUEST_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndSpecifiedTimeout() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg msg = TbMsg.newMsg(TbMsgType.REST_API_REQUEST, deviceId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), TbMsgMetaData.EMPTY, REQUEST_BODY);
        mockSuccessfulRestApiCallToRuleEngine(msg);

        var response = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId() + "/15000", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(Objects.requireNonNull(JacksonUtil.toString(response))).isEqualTo(REQUEST_BODY);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(), captor.capture(), eq(false), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getOriginator()).isEqualTo(deviceId);
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, REQUEST_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndResponseIsNull() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg msg = TbMsg.newMsg(TbMsgType.REST_API_REQUEST, deviceId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), TbMsgMetaData.EMPTY, REQUEST_BODY);
        mockSuccessfulRestApiCallToRuleEngine(null);

        doPostAsync("/api/rule-engine/DEVICE/" + deviceId.getId() + "/15000", REQUEST_BODY, String.class, status().isRequestTimeout());

        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(), captor.capture(), eq(false), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getOriginator()).isEqualTo(deviceId);
        Exception exception = new TimeoutException("Processing timeout detected!");
        testLogEntityActionError(null, deviceId, tenantId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, exception, REQUEST_BODY, "");
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndSpecifiedQueue() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg msg = TbMsg.newMsg("HighPriority", TbMsgType.REST_API_REQUEST, deviceId, TbMsgMetaData.EMPTY, REQUEST_BODY);
        mockSuccessfulRestApiCallToRuleEngine(msg);

        var response = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId() + "/HighPriority/1000", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(Objects.requireNonNull(JacksonUtil.toString(response))).isEqualTo(REQUEST_BODY);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(), captor.capture(), eq(true), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getQueueName()).isEqualTo(msg.getQueueName());
        assertThat(tbMsg.getOriginator()).isEqualTo(deviceId);
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(TenantId.SYS_TENANT_ID.getId()), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, REQUEST_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithInvalidRequestBody() throws Exception {
        loginSysAdmin();

        doPost("/api/rule-engine/", (Object) "@")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid request body")));

        verifyNoMoreInteractions(ruleEngineCallService);
    }

    @Test
    public void testHandleRuleEngineRequestWithAuthorityCustomerUser() throws Exception {
        loginTenantAdmin();
        Device device = assignDeviceToCustomer("test", "123", customerId);
        DeviceId deviceId = device.getId();
        loginCustomerUser();

        TbMsg msg = TbMsg.newMsg(TbMsgType.REST_API_REQUEST, deviceId, customerId, TbMsgMetaData.EMPTY, REQUEST_BODY);
        mockSuccessfulRestApiCallToRuleEngine(msg);

        var response = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId(), REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(Objects.requireNonNull(JacksonUtil.toString(response))).isEqualTo(REQUEST_BODY);
        ArgumentCaptor<TbMsg> captor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(), captor.capture(), eq(false), any());
        TbMsg tbMsg = captor.getValue();
        assertThat(tbMsg.getData()).isEqualTo(REQUEST_BODY);
        assertThat(tbMsg.getType()).isEqualTo(msg.getType());
        assertThat(tbMsg.getOriginator()).isEqualTo(deviceId);
        testLogEntityAction(null, deviceId, tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL,
                ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, REQUEST_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithoutPermission() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("test", "123");
        loginCustomerUser();

        MvcResult result = doPost("/api/rule-engine/DEVICE/" + device.getId().getId(), (Object) REQUEST_BODY).andReturn();

        ResponseEntity response = (ResponseEntity) result.getAsyncResult();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Objects.requireNonNull(response.getBody()).toString()).isEqualTo("You don't have permission to perform this operation!");
        verify(ruleEngineCallService, never()).processRestApiCallToRuleEngine(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    public void testHandleRuleEngineRequestUnauthorized() throws Exception {
        doPost("/api/rule-engine/", (Object) REQUEST_BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(statusReason(containsString("Authentication failed")));
    }

    private void mockSuccessfulRestApiCallToRuleEngine(TbMsg msg) {
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(4);
            consumer.accept(msg);
            return null;
        }).when(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), any(TbMsg.class), anyBoolean(), any());
    }

    private UserId getCurrentUserId() {
        Jws<Claims> jwsClaims = jwtTokenFactory.parseTokenClaims(token);
        Claims claims = jwsClaims.getPayload();
        String userId = claims.get("userId", String.class);
        return UserId.fromString(userId);
    }
}
