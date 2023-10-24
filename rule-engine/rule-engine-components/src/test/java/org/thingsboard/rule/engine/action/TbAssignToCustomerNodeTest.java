/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAssignToCustomerNodeTest extends TbAbstractCustomerActionNodeTest<TbAssignToCustomerNode, TbAssignToCustomerNodeConfiguration> {

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbAssignToCustomerNode();
        config = new TbAssignToCustomerNodeConfiguration().defaultConfiguration();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbAssignToCustomerNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getCustomerNamePattern()).isEmpty();
        assertThat(defaultConfig.isCreateCustomerIfNotExists()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideUnsupportedTypeAndVerifyExceptionThrown")
    void givenOriginatorType_whenMsg_thenVerifyExceptionThrown(EntityType originatorType) {
        processGivenOriginatorType_whenMsg_thenVerifyExceptionThrown(originatorType);
    }

    @ParameterizedTest
    @MethodSource("provideSupportedTypeAndCustomerTitle")
    void givenOriginatorTypeAndCustomerTitle_whenMsg_thenVerifySuccessOutMsg(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);
        var customer = createCustomer(customerTitle);

        when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(eq(TENANT_ID), eq(customerTitle))).thenReturn(customer);
        Map<EntityType, Consumer<EntityId>> entityTypeToAssignConsumerMap = mockMethodCallsForSupportedTypes(true);
        entityTypeToAssignConsumerMap.get(type).accept(originator);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verifyMsgSuccess(msg);
        verifyNoMoreInteractions(ctxMock);
    }

    @ParameterizedTest
    @MethodSource("provideSupportedTypeAndCustomerTitle")
    void givenOriginatorTypeAndCustomerTitle_whenMsg_thenVerifyCustomerCreatedAndSuccessOutMsg(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getSelfId()).thenReturn(RULE_NODE_ID);

        config.setCreateCustomerIfNotExists(true);
        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);
        var customer = createCustomer(customerTitle);

        when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(eq(TENANT_ID), eq(customerTitle))).thenReturn(null);
        when(customerServiceMock.saveCustomer(any(Customer.class))).thenReturn(customer);
        Map<EntityType, Consumer<EntityId>> entityTypeToEntityIdConsumerMap = mockMethodCallsForSupportedTypes(true);
        entityTypeToEntityIdConsumerMap.get(type).accept(originator);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Runnable>  runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(ctxMock, times(1))
                .enqueue(any(), runnableCaptor.capture(), any());
        runnableCaptor.getValue().run();
        verify(ctxMock, times(1)).customerCreatedMsg(any(), eq(RULE_NODE_ID));
        verifyMsgSuccess(msg);
    }

    @ParameterizedTest
    @MethodSource("provideSupportedTypeAndCustomerTitle")
    void givenOriginatorTypeAndCustomerTitle_whenMsg_thenVerifyCustomerNotFound(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);

        when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(eq(TENANT_ID), eq(customerTitle))).thenReturn(null);

        // WHEN
        var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getCause().getMessage()).isEqualTo("No customer found with name '" + customerTitle + "'.");
        verifyNoMoreInteractions(customerServiceMock);
        verifyNoMoreInteractions(ctxMock);
    }

}
