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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAssignToCustomerNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.DEVICE, EntityType.ASSET,
            EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.DASHBOARD);

    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(", "));

    private static final Set<EntityType> unsupportedEntityTypes = Arrays.stream(EntityType.values())
            .filter(type -> !supportedEntityTypes.contains(type)).collect(Collectors.toUnmodifiableSet());

    private final Device DEVICE = new Device();
    private final Asset ASSET = new Asset();
    private final EntityView ENTITY_VIEW = new EntityView();
    private final Edge EDGE = new Edge();
    private final Dashboard DASHBOARD = new Dashboard();

    private final TenantId TENANT_ID = new TenantId(UUID.fromString("c818385f-e661-407f-8c52-daf2dddf406d"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("c3570bd0-c0bc-4609-97a4-6f57d7c8b809"));

    private static Stream<Arguments> givenUnsupportedOriginatorType_whenOnMsg_thenVerifyExceptionThrown() {
        return unsupportedEntityTypes.stream().flatMap(type -> Stream.of(Arguments.of(type)));
    }

    private static Stream<Arguments> givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerify() {
        return supportedEntityTypes.stream()
                .flatMap(type -> Stream.of(Arguments.of(type, StringUtils.randomAlphabetic(5))));
    }

    private TbAssignToCustomerNode node;
    private TbAssignToCustomerNodeConfiguration config;

    @Mock
    private TbContext ctxMock;

    @Mock
    private CustomerService customerServiceMock;

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

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = spy(new TbAssignToCustomerNode());
        config = new TbAssignToCustomerNodeConfiguration().defaultConfiguration();
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbAssignToCustomerNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getCustomerNamePattern()).isEmpty();
        assertThat(defaultConfig.isCreateCustomerIfNotExists()).isFalse();
    }

    @ParameterizedTest
    @MethodSource
    void givenUnsupportedOriginatorType_whenOnMsg_thenVerifyExceptionThrown(EntityType originatorType) {
        // GIVEN
        var originator = toOriginator(originatorType);
        var msg = getTbMsg(originator);

        // WHEN
        var exception = assertThrows(RuntimeException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Unsupported originator type '" + originatorType +
                "'! Only " + supportedEntityTypesStr + " types are allowed.");
        verifyNoInteractions(ctxMock);
        verifyNoInteractions(customerServiceMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerify")
    void givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerify(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);
        var customer = createCustomer(customerTitle);

        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(eq(TENANT_ID), eq(customerTitle)))
                .thenReturn(Futures.immediateFuture(Optional.of(customer)));
        Map<EntityType, Consumer<EntityId>> entityTypeToAssignConsumerMap = mockMethodCallsForSupportedTypes();
        entityTypeToAssignConsumerMap.get(type).accept(originator);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verifyMsgSuccess(msg);
        verifyNoMoreInteractions(ctxMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerify")
    void givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerifyCustomerCreatedAndSuccessOutMsg(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(ctxMock.getSelfId()).thenReturn(RULE_NODE_ID);

        config.setCreateCustomerIfNotExists(true);
        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);
        var customer = createCustomer(customerTitle);

        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(eq(TENANT_ID), eq(customerTitle)))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        when(customerServiceMock.saveCustomer(any(Customer.class))).thenReturn(customer);
        Map<EntityType, Consumer<EntityId>> entityTypeToEntityIdConsumerMap = mockMethodCallsForSupportedTypes();
        entityTypeToEntityIdConsumerMap.get(type).accept(originator);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(ctxMock).enqueue(any(), runnableCaptor.capture(), any());
        runnableCaptor.getValue().run();
        verify(ctxMock).customerCreatedMsg(any(), eq(RULE_NODE_ID));
        verifyMsgSuccess(msg);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerify")
    void givenSupportedOriginatorTypeAndCustomerTitle_whenOnMsg_thenVerifyCustomerNotFound(EntityType type, String customerTitle) throws TbNodeException {
        // GIVEN

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);

        config.setCustomerNamePattern(customerTitle);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        var originator = toOriginator(type);
        var msg = getTbMsg(originator);

        when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(eq(TENANT_ID), eq(customerTitle)))
                .thenReturn(Futures.immediateFuture(Optional.empty()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        assertThat(throwableCaptor.getValue()).hasMessage("Customer with title '" + customerTitle + "' doesn't exist!");

        verifyNoMoreInteractions(customerServiceMock);
        verifyNoMoreInteractions(ctxMock);
    }

    private Map<EntityType, Consumer<EntityId>> mockMethodCallsForSupportedTypes() {
        return Map.of(
                EntityType.DEVICE, id -> {
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(deviceServiceMock.assignDeviceToCustomer(eq(TENANT_ID), (DeviceId) eq(id), any()))
                            .thenReturn(DEVICE);
                },
                EntityType.ASSET, id -> {
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.assignAssetToCustomer(eq(TENANT_ID), (AssetId) eq(id), any()))
                            .thenReturn(ASSET);
                },
                EntityType.ENTITY_VIEW, id -> {
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    when(entityViewServiceMock.assignEntityViewToCustomer(eq(TENANT_ID), (EntityViewId) eq(id), any()))
                            .thenReturn(ENTITY_VIEW);
                },
                EntityType.EDGE, id -> {
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    when(edgeServiceMock.assignEdgeToCustomer(eq(TENANT_ID), (EdgeId) eq(id), any()))
                            .thenReturn(EDGE);
                },
                EntityType.DASHBOARD, id -> {
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    when(dashboardServiceMock.assignDashboardToCustomer(eq(TENANT_ID), (DashboardId) eq(id), any()))
                            .thenReturn(DASHBOARD);
                }
        );
    }

    private void verifyMsgSuccess(TbMsg expectedMsg) {
        verify(ctxMock).tellSuccess(eq(expectedMsg));
        verify(ctxMock, never()).tellFailure(any(), any());
    }

    private Customer createCustomer(String customerTitle) {
        var customer = new Customer();
        customer.setTitle(customerTitle);
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTenantId(TENANT_ID);
        return customer;
    }

    private TbMsg getTbMsg(EntityId originator) {
        return TbMsg.newMsg()
                .type(TbMsgType.NA)
                .originator(originator)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
    }

    private EntityId toOriginator(EntityType type) {
        return EntityIdFactory.getByTypeAndId(type.name(), UUID.randomUUID().toString());
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"customerNamePattern\":\"\",\"createCustomerIfNotExists\":\"false\",\"customerCacheExpiration\":300}",
                        true,
                        "{\"customerNamePattern\":\"\",\"createCustomerIfNotExists\":\"false\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"customerNamePattern\":\"\",\"createCustomerIfNotExists\":\"false\"}",
                        false,
                        "{\"customerNamePattern\":\"\",\"createCustomerIfNotExists\":\"false\"}")
        );
    }

}
