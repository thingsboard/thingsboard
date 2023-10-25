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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAbstractCustomerActionNodeTest<N extends TbAbstractCustomerActionNode<C>, C extends TbAbstractCustomerActionNodeConfiguration> {

    private static final Device DEVICE = new Device();
    private static final Asset ASSET = new Asset();
    private static final EntityView ENTITY_VIEW = new EntityView();
    private static final Edge EDGE = new Edge();
    private static final Dashboard DASHBOARD = new Dashboard();

    private static final List<EntityType> supportedEntityTypes = List.of(EntityType.DEVICE, EntityType.ASSET,
            EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.DASHBOARD);

    private static final List<EntityType> unsupportedEntityTypes = Arrays.stream(EntityType.values())
            .filter(type -> !supportedEntityTypes.contains(type)).collect(Collectors.toList());

    protected static final String CUSTOMER_CACHE_EXPIRATION_FIELD = "customerCacheExpiration";
    protected static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    protected static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    protected static final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.randomUUID());

    protected static Stream<Arguments> provideUnsupportedTypeAndVerifyExceptionThrown() {
        return unsupportedEntityTypes.stream().flatMap(type -> Stream.of(Arguments.of(type)));
    }

    protected static Stream<Arguments> provideSupportedTypeAndCustomerTitle() {
        return supportedEntityTypes.stream()
                .flatMap(type -> Stream.of(Arguments.of(type, StringUtils.randomAlphabetic(5))));
    }

    protected N node;
    protected C config;

    @Mock
    protected TbContext ctxMock;

    @Mock
    protected CustomerService customerServiceMock;

    @Mock
    private DeviceService deviceServiceMock;

    @Mock
    private AssetService assetServiceMock;

    @Mock
    private EntityViewService entityViewServiceMock;

    @Mock
    private EdgeService edgeServiceMock;

    @Mock
    private DashboardService dashboardServiceMock;

    protected void processGivenOriginatorType_whenMsg_thenVerifyExceptionThrown(EntityType originatorType) {
        // GIVEN
        var originator = toOriginator(originatorType);
        var msg = getTbMsg(originator);

        // WHEN
        var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo(N.unsupportedOriginatorTypeErrorMessage(originatorType));
        verifyNoInteractions(ctxMock);
        verifyNoInteractions(customerServiceMock);
    }

    protected Map<EntityType, Consumer<EntityId>> mockMethodCallsForSupportedTypes(boolean assign) {
        return Map.of(
                EntityType.DEVICE, id -> {
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    if (assign) {
                        when(deviceServiceMock.assignDeviceToCustomer(eq(TENANT_ID), (DeviceId) eq(id), any()))
                                .thenReturn(DEVICE);
                    } else {
                        when(deviceServiceMock.unassignDeviceFromCustomer(eq(TENANT_ID), (DeviceId) eq(id)))
                                .thenReturn(DEVICE);
                    }
                },
                EntityType.ASSET, id -> {
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    if (assign) {
                        when(assetServiceMock.assignAssetToCustomer(eq(TENANT_ID), (AssetId) eq(id), any()))
                                .thenReturn(ASSET);
                    } else {
                        when(assetServiceMock.unassignAssetFromCustomer(eq(TENANT_ID), (AssetId) eq(id)))
                                .thenReturn(ASSET);
                    }
                },
                EntityType.ENTITY_VIEW, id -> {
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    if (assign) {
                        when(entityViewServiceMock.assignEntityViewToCustomer(eq(TENANT_ID), (EntityViewId) eq(id), any()))
                                .thenReturn(ENTITY_VIEW);
                    } else {
                        when(entityViewServiceMock.unassignEntityViewFromCustomer(eq(TENANT_ID), (EntityViewId) eq(id)))
                                .thenReturn(ENTITY_VIEW);
                    }
                },
                EntityType.EDGE, id -> {
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    if (assign) {
                        when(edgeServiceMock.assignEdgeToCustomer(eq(TENANT_ID), (EdgeId) eq(id), any()))
                                .thenReturn(EDGE);
                    } else {
                        when(edgeServiceMock.unassignEdgeFromCustomer(eq(TENANT_ID), (EdgeId) eq(id)))
                                .thenReturn(EDGE);
                    }
                },
                EntityType.DASHBOARD, id -> {
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    if (assign) {
                        when(dashboardServiceMock.assignDashboardToCustomer(eq(TENANT_ID), (DashboardId) eq(id), any()))
                                .thenReturn(DASHBOARD);
                    } else {
                        when(dashboardServiceMock.unassignDashboardFromCustomer(eq(TENANT_ID), (DashboardId) eq(id), any()))
                                .thenReturn(DASHBOARD);
                    }
                }
        );
    }

    protected void processTestUpgrade_fromVersion0(N node, C config) {
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(config);
        jsonNode.put(CUSTOMER_CACHE_EXPIRATION_FIELD, 300);
        assertThat(jsonNode.has(CUSTOMER_CACHE_EXPIRATION_FIELD)).as("pre condition has " + CUSTOMER_CACHE_EXPIRATION_FIELD).isTrue();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isTrue();
        assertThat(resultNode.has(CUSTOMER_CACHE_EXPIRATION_FIELD)).as("upgrade result has no key " + CUSTOMER_CACHE_EXPIRATION_FIELD).isFalse();
    }

    protected void processTestUpgrade_fromVersion0_alreadyHasLatestConfig(N node, C config) {
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(config);
        assertThat(jsonNode.has(CUSTOMER_CACHE_EXPIRATION_FIELD)).as("pre condition has no " + CUSTOMER_CACHE_EXPIRATION_FIELD).isFalse();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isFalse();
        assertThat(resultNode.has(CUSTOMER_CACHE_EXPIRATION_FIELD)).as("upgrade result has no key " + CUSTOMER_CACHE_EXPIRATION_FIELD).isFalse();
    }

    protected void verifyMsgSuccess(TbMsg expectedMsg) {
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock, times(1)).tellSuccess(msgCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());
        TbMsg actualValue = msgCaptor.getValue();
        assertThat(actualValue).isSameAs(expectedMsg);
    }

    protected Customer createCustomer(String customerTitle) {
        var customer = new Customer();
        customer.setTitle(customerTitle);
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTenantId(TENANT_ID);
        return customer;
    }

    protected TbMsg getTbMsg(EntityId originator) {
        return TbMsg.newMsg(TbMsgType.NA, originator, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

    protected static EntityId toOriginator(EntityType type) {
        return EntityIdFactory.getByTypeAndId(type.name(), UUID.randomUUID().toString());
    }

}
