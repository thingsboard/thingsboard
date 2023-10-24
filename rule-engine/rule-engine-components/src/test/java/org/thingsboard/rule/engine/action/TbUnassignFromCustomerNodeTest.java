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
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbUnassignFromCustomerNodeTest extends TbAbstractCustomerActionNodeTest<TbUnassignFromCustomerNode, TbUnassignFromCustomerNodeConfiguration> {

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbUnassignFromCustomerNode();
        config = new TbUnassignFromCustomerNodeConfiguration().defaultConfiguration();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbUnassignFromCustomerNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getCustomerNamePattern()).isEmpty();
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

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);

        // we search for the customer only if incoming message originator is dashboard.
        if (type.equals(EntityType.DASHBOARD)) {
            when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
            var customer = createCustomer(customerTitle);
            when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(eq(TENANT_ID), eq(customerTitle))).thenReturn(customer);
        }
        Map<EntityType, Consumer<EntityId>> entityTypeToEntityIdConsumerMap = mockMethodCallsForSupportedTypes(false);
        entityTypeToEntityIdConsumerMap.get(type).accept(originator);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verifyMsgSuccess(msg);
        verifyNoMoreInteractions(ctxMock);
    }

    @ParameterizedTest
    @MethodSource("provideSupportedTypeAndCustomerTitle")
    void givenOriginatorTypeAndCustomerTitle_whenMsg_thenVerifyCustomerSearchedAndNotFoundOnlyForDashboardOriginator(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);

        // we search for the customer only if incoming message originator is dashboard.
        if (type.equals(EntityType.DASHBOARD)) {
            when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
            when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(eq(TENANT_ID), eq(customerTitle))).thenReturn(null);

            // DASHBOARD WHEN
            var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

            // DASHBOARD THEN
            assertThat(exception.getCause().getMessage()).isEqualTo("No customer found with name '" + customerTitle + "'.");
            verifyNoMoreInteractions(customerServiceMock);
            verifyNoMoreInteractions(ctxMock);
            return;
        }
        Map<EntityType, Consumer<EntityId>> entityTypeToEntityIdConsumerMap = mockMethodCallsForSupportedTypes(false);
        entityTypeToEntityIdConsumerMap.get(type).accept(originator);

        // OTHER TYPES WHEN
        node.onMsg(ctxMock, msg);

        // OTHER TYPES THEN
        verifyMsgSuccess(msg);
        verifyNoMoreInteractions(ctxMock);
    }

}
