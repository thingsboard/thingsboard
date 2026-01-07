/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class RuleEngineControllerTest extends AbstractControllerTest {

    private final String REQUEST_BODY = "{\"request\":\"download\"}";
    private final String RESPONSE_BODY = "{\"response\":\"downloadOk\"}";

    @SpyBean
    private RuleEngineCallService ruleEngineCallService;

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorUser() throws Exception {
        loginSysAdmin();
        TbMsg responseMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(currentUserId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        JsonNode apiResponse = doPostAsyncWithTypedResponse("/api/rule-engine/", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);
        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(TenantId.SYS_TENANT_ID), any(UUID.class), requestMsgCaptor.capture(), eq(false), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(currentUserId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isNull();
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        testLogEntityAction(null, currentUserId, TenantId.SYS_TENANT_ID, new CustomerId(EntityId.NULL_UUID), currentUserId,
                SYS_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, RESPONSE_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDevice() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg responseMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        JsonNode apiResponse = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId(), REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);
        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), requestMsgCaptor.capture(), eq(false), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(deviceId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isNull();
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(EntityId.NULL_UUID), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, RESPONSE_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndSpecifiedTimeout() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg responseMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        JsonNode apiResponse = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId() + "/15000", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);
        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), requestMsgCaptor.capture(), eq(false), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(deviceId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isNull();
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(EntityId.NULL_UUID), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, RESPONSE_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndResponseIsNull() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        mockRestApiCallToRuleEngine(null);

        doPostAsync("/api/rule-engine/DEVICE/" + deviceId.getId() + "/15000", REQUEST_BODY, String.class, status().isRequestTimeout());

        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), requestMsgCaptor.capture(), eq(false), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(deviceId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isNull();
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        Exception exception = new TimeoutException("Processing timeout detected!");
        testLogEntityActionError(deviceId, tenantId, new CustomerId(EntityId.NULL_UUID), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, exception, REQUEST_BODY, "");
    }

    @Test
    public void testHandleRuleEngineRequestWithMsgOriginatorDeviceAndSpecifiedQueue() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        TbMsg responseMsg = TbMsg.newMsg()
                .queueName(DataConstants.HP_QUEUE_NAME)
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        JsonNode apiResponse = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId() + "/HighPriority/1000", REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);
        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), requestMsgCaptor.capture(), eq(true), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getQueueName()).isEqualTo(DataConstants.HP_QUEUE_NAME);
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(deviceId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isNull();
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        testLogEntityAction(null, deviceId, tenantId, new CustomerId(EntityId.NULL_UUID), tenantAdminUserId,
                TENANT_ADMIN_EMAIL, ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, RESPONSE_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithInvalidRequestBody() throws Exception {
        loginSysAdmin();

        doPost("/api/rule-engine/", (Object) "@")
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid request body")));

        verifyNoInteractions(ruleEngineCallService);
    }

    @Test
    public void testHandleRuleEngineRequestWithAuthorityCustomerUser() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("Test", "123");
        DeviceId deviceId = device.getId();
        assignDeviceToCustomer(deviceId, customerId);
        loginCustomerUser();

        TbMsg responseMsg = TbMsg.newMsg()
                .type(TbMsgType.REST_API_REQUEST)
                .originator(deviceId)
                .customerId(customerId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(RESPONSE_BODY)
                .build();
        mockRestApiCallToRuleEngine(responseMsg);

        JsonNode apiResponse = doPostAsyncWithTypedResponse("/api/rule-engine/DEVICE/" + deviceId.getId(), REQUEST_BODY, new TypeReference<>() {
        }, status().isOk());

        assertThat(JacksonUtil.toString(apiResponse)).isEqualTo(RESPONSE_BODY);
        ArgumentCaptor<TbMsg> requestMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ruleEngineCallService).processRestApiCallToRuleEngine(eq(tenantId), any(UUID.class), requestMsgCaptor.capture(), eq(false), any(Consumer.class));
        TbMsg requestMsgCaptorValue = requestMsgCaptor.getValue();
        assertThat(requestMsgCaptorValue.getData()).isEqualTo(REQUEST_BODY);
        assertThat(requestMsgCaptorValue.getType()).isEqualTo(TbMsgType.REST_API_REQUEST.name());
        assertThat(requestMsgCaptorValue.getOriginator()).isEqualTo(deviceId);
        assertThat(requestMsgCaptorValue.getCustomerId()).isEqualTo(customerId);
        checkMetadataProperties(requestMsgCaptorValue.getMetaData());
        testLogEntityAction(null, deviceId, tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL,
                ActionType.REST_API_RULE_ENGINE_CALL, 1, REQUEST_BODY, RESPONSE_BODY);
    }

    @Test
    public void testHandleRuleEngineRequestWithoutPermission() throws Exception {
        loginTenantAdmin();
        Device device = createDevice("test", "123");
        loginCustomerUser();

        doPostAsync("/api/rule-engine/DEVICE/" + device.getId().getId(), (Object) REQUEST_BODY, -1L)
                .andExpect(status().isForbidden())
                .andExpect(content().string("You don't have permission to perform this operation!"));

        verifyNoInteractions(ruleEngineCallService);
    }

    @Test
    public void testHandleRuleEngineRequestUnauthorized() throws Exception {
        doPost("/api/rule-engine/", (Object) REQUEST_BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(statusReason(containsString("Authentication failed")));
    }

    private void mockRestApiCallToRuleEngine(TbMsg responseMsg) {
        doAnswer(invocation -> {
            Consumer<TbMsg> consumer = invocation.getArgument(4);
            consumer.accept(responseMsg);
            return null;
        }).when(ruleEngineCallService).processRestApiCallToRuleEngine(any(TenantId.class), any(UUID.class), any(TbMsg.class), anyBoolean(), any(Consumer.class));
    }

    public void checkMetadataProperties(TbMsgMetaData metaData) {
        Map<String, String> data = metaData.getData();
        assertThat(data).containsKeys("serviceId", "requestUUID", "expirationTime");
    }
}
