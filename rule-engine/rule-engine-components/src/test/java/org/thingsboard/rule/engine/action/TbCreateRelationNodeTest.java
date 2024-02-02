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
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbCreateRelationNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.DEVICE,
            EntityType.ASSET, EntityType.CUSTOMER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE, EntityType.USER);

    private static final Set<EntityType> unsupportedEntityTypes = Arrays.stream(EntityType.values())
            .filter(type -> !supportedEntityTypes.contains(type)).collect(Collectors.toUnmodifiableSet());

    private static Stream<Arguments> givenSupportedEntityType_whenOnMsg_thenVerifyEntityNotFoundExceptionThrown() {
        return supportedEntityTypes.stream().filter(entityType -> !entityType.equals(EntityType.TENANT)).map(Arguments::of);
    }

    private static final TenantId tenantId = new TenantId(UUID.fromString("6fc86fc9-b25c-4893-b340-51cf4e101ab2"));
    private static final DeviceId deviceId = new DeviceId(UUID.fromString("191ab124-1d8d-4749-97c6-fc84c113c1f5"));
    private static final AssetId assetId = new AssetId(UUID.fromString("a47a5867-deab-4333-b845-88cb1695990c"));
    private static final CustomerId customerId = new CustomerId(UUID.fromString("4af69229-273d-40de-9fba-f49f87373d23"));
    private static final EntityViewId entityViewId = new EntityViewId(UUID.fromString("d4c22c9c-07f5-474d-9d16-b63f0e71f914"));
    private static final EdgeId edgeId = new EdgeId(UUID.fromString("7c653959-558d-4661-aac7-c1866eef286b"));
    private static final DashboardId dashboardId = new DashboardId(UUID.fromString("6fcfbcb0-21e4-4b0b-a0d6-399ca6959cb2"));

    private final DeviceId originatorId = new DeviceId(UUID.fromString("860634b1-8a1e-4693-9ae8-e779c7f5f4da"));
    private final RuleNodeId ruleNodeId = new RuleNodeId(UUID.fromString("d05a0491-ee7a-484a-8c1b-91111ef39287"));

    private static Stream<Arguments> givenSupportedEntityType_whenOnMsg_thenVerifyConditions() {
        return Stream.of(
                Arguments.of(EntityType.DEVICE, new Device(deviceId)),
                Arguments.of(EntityType.ASSET, new Asset(assetId)),
                Arguments.of(EntityType.CUSTOMER, new Customer(customerId)),
                Arguments.of(EntityType.ENTITY_VIEW, new EntityView(entityViewId)),
                Arguments.of(EntityType.EDGE, new Edge(edgeId)),
                Arguments.of(EntityType.DASHBOARD, new Dashboard(dashboardId)),
                Arguments.of(EntityType.TENANT, new Tenant(tenantId))
        );
    }

    private static Stream<Arguments> givenSupportedEntityTypeToCreateEntityIfNotExists_whenOnMsg_thenVerifyConditions() {
        return Stream.of(
                Arguments.of(EntityType.DEVICE, new Device(deviceId)),
                Arguments.of(EntityType.ASSET, new Asset(assetId)),
                Arguments.of(EntityType.CUSTOMER, new Customer(customerId))
        );
    }

    @Mock
    private TbContext ctxMock;
    @Mock
    private AssetService assetServiceMock;
    @Mock
    private DeviceService deviceServiceMock;
    @Mock
    private EntityViewService entityViewServiceMock;
    @Mock
    private CustomerService customerServiceMock;
    @Mock
    private EdgeService edgeServiceMock;
    @Mock
    private UserService userServiceMock;
    @Mock
    private DashboardService dashboardServiceMock;
    @Mock
    private TenantService tenantServiceMock;
    @Mock
    private TbClusterService clusterServiceMock;
    @Mock
    private RelationService relationServiceMock;

    private final ListeningExecutor dbExecutor = new TestDbCallbackExecutor();

    private TbCreateRelationNode node;
    private TbCreateRelationNodeConfiguration config;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = spy(new TbCreateRelationNode());
        config = new TbCreateRelationNodeConfiguration().defaultConfiguration();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbCreateRelationNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getDirection()).isEqualTo(EntitySearchDirection.FROM.name());
        assertThat(defaultConfig.getRelationType()).isEqualTo(EntityRelation.CONTAINS_TYPE);
        assertThat(defaultConfig.getEntityNamePattern()).isEqualTo("");
        assertThat(defaultConfig.getEntityTypePattern()).isEqualTo(null);
        assertThat(defaultConfig.getEntityType()).isEqualTo(null);
        assertThat(defaultConfig.isCreateEntityIfNotExists()).isFalse();
        assertThat(defaultConfig.isRemoveCurrentRelations()).isFalse();
        assertThat(defaultConfig.isChangeOriginatorToRelatedEntity()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void givenEntityType_whenInit_thenVerifyExceptionThrownIfTypeIsUnsupported(EntityType entityType) {
        // GIVEN
        config.setEntityType(entityType.name());
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        if (unsupportedEntityTypes.contains(entityType)) {
            assertThatThrownBy(() -> node.init(ctxMock, nodeConfiguration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage(TbAbstractRelationActionNode.unsupportedEntityTypeErrorMessage(entityType.name()));
        } else {
            assertThatCode(() -> node.init(ctxMock, nodeConfiguration)).doesNotThrowAnyException();
        }
        verifyNoInteractions(ctxMock);
    }

    @ParameterizedTest
    @MethodSource
    void givenSupportedEntityType_whenOnMsg_thenVerifyEntityNotFoundExceptionThrown(EntityType entityType) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType.name());
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        var mockMethodCallsMap = mockEntityServiceCallsEntityNotFound();
        mockMethodCallsMap.get(entityType).run();

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN-THEN
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(NoSuchElementException.class);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsg_thenVerifyRelationCreatedAndOutMsgSuccess(EntityType entityType, HasId entity) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType.name());
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setCreateEntityIfNotExists(false);
        config.setChangeOriginatorToRelatedEntity(false);
        config.setRemoveCurrentRelations(false);

        var entityId = (EntityId) entity.getId();

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCallsCreateEntityIfNotExistsDisabled();
        mockMethodCallsMap.get(entityType).accept(entity);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));
        when(relationServiceMock.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCallsCreateEntityIfNotExistsDisabled();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(entityId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).saveRelationAsync(eq(tenantId), eq(new EntityRelation(entityId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)));

        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.SUCCESS));
        verify(ctxMock, never()).tellFailure(any(), any());
        switch (entityType) {
            case DEVICE:
            case TENANT:
                verify(ctxMock, times(6)).getDbCallbackExecutor();
                break;
            default:
                verify(ctxMock, times(7)).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsg_thenVerifyDeleteCurrentRelationsCreateNewRelationAndOutMsgSuccess(EntityType entityType, HasId entity) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType.name());
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setCreateEntityIfNotExists(false);
        config.setChangeOriginatorToRelatedEntity(false);
        config.setRemoveCurrentRelations(true);

        var entityId = (EntityId) entity.getId();

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCallsCreateEntityIfNotExistsDisabled();
        mockMethodCallsMap.get(entityType).accept(entity);

        var relationToDelete = new EntityRelation();
        when(relationServiceMock.findByToAndTypeAsync(any(), any(), any(), any())).thenReturn(Futures.immediateFuture(List.of(relationToDelete)));
        when(relationServiceMock.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));
        when(relationServiceMock.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCallsCreateEntityIfNotExistsDisabled();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).findByToAndTypeAsync(eq(tenantId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).deleteRelationAsync(eq(tenantId), eq(relationToDelete));
        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(entityId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).saveRelationAsync(eq(tenantId), eq(new EntityRelation(entityId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)));

        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.SUCCESS));
        verify(ctxMock, never()).tellFailure(any(), any());
        switch (entityType) {
            case DEVICE:
            case TENANT:
                verify(ctxMock, times(8)).getDbCallbackExecutor();
                break;
            default:
                verify(ctxMock, times(9)).getDbCallbackExecutor();
                break;
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsg_thenVerifyRelationCreatedAndOriginatorChanged(EntityType entityType, HasId entity) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType.name());
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setCreateEntityIfNotExists(false);
        config.setChangeOriginatorToRelatedEntity(true);
        config.setRemoveCurrentRelations(false);

        var entityId = (EntityId) entity.getId();

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCallsCreateEntityIfNotExistsDisabled();
        mockMethodCallsMap.get(entityType).accept(entity);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));
        when(relationServiceMock.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        var msgAfterOriginatorChanged = TbMsg.transformMsgOriginator(msg, originatorId);
        when(ctxMock.transformMsgOriginator(any(), any())).thenReturn(msgAfterOriginatorChanged);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCallsCreateEntityIfNotExistsDisabled();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(entityId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).saveRelationAsync(eq(tenantId), eq(new EntityRelation(entityId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)));

        verify(ctxMock).transformMsgOriginator(eq(msg), eq(entityId));
        verify(ctxMock).tellNext(eq(msgAfterOriginatorChanged), eq(TbNodeConnectionType.SUCCESS));
        verify(ctxMock, never()).tellFailure(any(), any());
        switch (entityType) {
            case DEVICE:
            case TENANT:
                verify(ctxMock, times(6)).getDbCallbackExecutor();
                break;
            default:
                verify(ctxMock, times(7)).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityTypeToCreateEntityIfNotExists_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsg_thenVerifyRelationAndEntityCreatedAndOutMsgSuccess(EntityType entityType, HasId entity) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType.name());
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setCreateEntityIfNotExists(true);
        config.setChangeOriginatorToRelatedEntity(false);
        config.setRemoveCurrentRelations(false);

        var entityId = (EntityId) entity.getId();

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getSelfId()).thenReturn(ruleNodeId);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCallsCreateEntityIfNotExistsEnabled();
        var entityCreatedMsg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
        mockMethodCallsMap.get(entityType).accept(entity, entityCreatedMsg);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));
        when(relationServiceMock.saveRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCallsCreateEntityIfNotExistsEnabled();
        verifyMethodCallsMap.get(entityType).accept(entity, entityCreatedMsg);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(entityId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).saveRelationAsync(eq(tenantId), eq(new EntityRelation(entityId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON)));

        verify(ctxMock).tellNext(eq(msg), eq(TbNodeConnectionType.SUCCESS));
        verify(ctxMock, never()).tellFailure(any(), any());
        if (entityType == EntityType.DEVICE) {
            verify(ctxMock, times(6)).getDbCallbackExecutor();
        } else {
            verify(ctxMock, times(7)).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    private Map<EntityType, Consumer<HasId>> mockEntityServiceCallsCreateEntityIfNotExistsDisabled() {
        return Map.of(
                EntityType.DEVICE, hasId -> {
                    var device = (Device) hasId;
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(deviceServiceMock.findDeviceByTenantIdAndName(any(), any())).thenReturn(device);
                    when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
                },
                EntityType.ASSET, hasId -> {
                    var asset = (Asset) hasId;
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.findAssetByTenantIdAndName(any(), any())).thenReturn(asset);
                    when(assetServiceMock.findAssetByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(asset));
                },
                EntityType.CUSTOMER, hasId -> {
                    var customer = (Customer) hasId;
                    when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                    when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(any(), any())).thenReturn(customer);
                    when(customerServiceMock.findCustomerByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(customer));
                },
                EntityType.ENTITY_VIEW, hasId -> {
                    var entityView = (EntityView) hasId;
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    when(entityViewServiceMock.findEntityViewByTenantIdAndName(any(), any())).thenReturn(entityView);
                    when(entityViewServiceMock.findEntityViewByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(entityView));
                },
                EntityType.EDGE, hasId -> {
                    var edge = (Edge) hasId;
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    when(edgeServiceMock.findEdgeByTenantIdAndName(any(), any())).thenReturn(edge);
                    when(edgeServiceMock.findEdgeByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(edge));
                },
                EntityType.USER, hasId -> {
                    var user = (User) hasId;
                    when(ctxMock.getUserService()).thenReturn(userServiceMock);
                    when(userServiceMock.findUserByTenantIdAndEmail(any(), any())).thenReturn(user);
                    when(userServiceMock.findUserByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(user));
                },
                EntityType.DASHBOARD, hasId -> {
                    var dashboard = (Dashboard) hasId;
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    when(dashboardServiceMock.findFirstDashboardInfoByTenantIdAndName(any(), any())).thenReturn(dashboard);
                    when(dashboardServiceMock.findDashboardByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(dashboard));
                },
                EntityType.TENANT, hasId -> {
                    var tenant = (Tenant) hasId;
                    when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
                    when(tenantServiceMock.findTenantByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(tenant));
                }
        );
    }

    private Map<EntityType, Consumer<HasId>> verifyEntityServiceCallsCreateEntityIfNotExistsDisabled() {
        return Map.of(
                EntityType.DEVICE, hasId -> {
                    var device = (Device) hasId;
                    verify(deviceServiceMock).findDeviceByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(deviceServiceMock).findDeviceById(eq(tenantId), eq(device.getId()));
                    verifyNoMoreInteractions(deviceServiceMock);
                },
                EntityType.ASSET, hasId -> {
                    var asset = (Asset) hasId;
                    verify(assetServiceMock).findAssetByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(assetServiceMock).findAssetByIdAsync(eq(tenantId), eq(asset.getId()));
                    verifyNoMoreInteractions(assetServiceMock);
                },
                EntityType.CUSTOMER, hasId -> {
                    var customer = (Customer) hasId;
                    verify(customerServiceMock).findCustomerByTenantIdAndTitleUsingCache(eq(tenantId), eq("EntityName"));
                    verify(customerServiceMock).findCustomerByIdAsync(eq(tenantId), eq(customer.getId()));
                    verifyNoMoreInteractions(customerServiceMock);
                },
                EntityType.ENTITY_VIEW, hasId -> {
                    var entityView = (EntityView) hasId;
                    verify(entityViewServiceMock).findEntityViewByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(entityViewServiceMock).findEntityViewByIdAsync(eq(tenantId), eq(entityView.getId()));
                    verifyNoMoreInteractions(entityViewServiceMock);
                },
                EntityType.EDGE, hasId -> {
                    var edge = (Edge) hasId;
                    verify(edgeServiceMock).findEdgeByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(edgeServiceMock).findEdgeByIdAsync(eq(tenantId), eq(edge.getId()));
                    verifyNoMoreInteractions(edgeServiceMock);
                },
                EntityType.USER, hasId -> {
                    var user = (User) hasId;
                    verify(userServiceMock).findUserByTenantIdAndEmail(eq(tenantId), eq("EntityName"));
                    verify(userServiceMock).findUserByIdAsync(eq(tenantId), eq(user.getId()));
                    verifyNoMoreInteractions(userServiceMock);
                },
                EntityType.DASHBOARD, hasId -> {
                    var dashboard = (Dashboard) hasId;
                    verify(dashboardServiceMock).findFirstDashboardInfoByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(dashboardServiceMock).findDashboardByIdAsync(eq(tenantId), eq(dashboard.getId()));
                    verifyNoMoreInteractions(dashboardServiceMock);
                },
                EntityType.TENANT, hasId -> {
                    var tenant = (Tenant) hasId;
                    verify(tenantServiceMock).findTenantByIdAsync(eq(tenantId), eq(tenant.getId()));
                }
        );
    }

    private Map<EntityType, BiConsumer<HasId, TbMsg>> mockEntityServiceCallsCreateEntityIfNotExistsEnabled() {
        return Map.of(
                EntityType.DEVICE, (hasId, entityCreatedMsg) -> {
                    var device = (Device) hasId;
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(ctxMock.getClusterService()).thenReturn(clusterServiceMock);
                    when(deviceServiceMock.findDeviceByTenantIdAndName(any(), any())).thenReturn(null);
                    when(deviceServiceMock.saveDevice(any())).thenReturn(device);
                    when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
                    doAnswer(invocation -> entityCreatedMsg).when(ctxMock).deviceCreatedMsg(any(), any());
                },
                EntityType.ASSET, (hasId, entityCreatedMsg) -> {
                    var asset = (Asset) hasId;
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.findAssetByTenantIdAndName(any(), any())).thenReturn(null);
                    when(assetServiceMock.saveAsset(any())).thenReturn(asset);
                    when(assetServiceMock.findAssetByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(asset));
                    doAnswer(invocation -> entityCreatedMsg).when(ctxMock).assetCreatedMsg(any(), any());
                },
                EntityType.CUSTOMER, (hasId, entityCreatedMsg) -> {
                    var customer = (Customer) hasId;
                    when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                    when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(any(), any())).thenReturn(null);
                    when(customerServiceMock.saveCustomer(any())).thenReturn(customer);
                    when(customerServiceMock.findCustomerByIdAsync(any(), any())).thenReturn(Futures.immediateFuture(customer));
                    doAnswer(invocation -> entityCreatedMsg).when(ctxMock).customerCreatedMsg(any(), any());
                }
        );
    }

    private Map<EntityType, BiConsumer<HasId, TbMsg>> verifyEntityServiceCallsCreateEntityIfNotExistsEnabled() {
        return Map.of(
                EntityType.DEVICE, (hasId, entityCreatedMsg) -> {
                    var device = (Device) hasId;
                    verify(deviceServiceMock).findDeviceByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(deviceServiceMock).saveDevice(any());
                    verify(clusterServiceMock).onDeviceUpdated(eq(device), eq(null));
                    verify(ctxMock).enqueue(eq(entityCreatedMsg), any(), any());
                    verify(deviceServiceMock).findDeviceById(eq(tenantId), eq(device.getId()));
                    verifyNoMoreInteractions(deviceServiceMock, clusterServiceMock);
                },
                EntityType.ASSET, (hasId, entityCreatedMsg) -> {
                    var asset = (Asset) hasId;
                    verify(assetServiceMock).findAssetByTenantIdAndName(eq(tenantId), eq("EntityName"));
                    verify(assetServiceMock).saveAsset(any());
                    verify(ctxMock).enqueue(eq(entityCreatedMsg), any(), any());
                    verify(assetServiceMock).findAssetByIdAsync(eq(tenantId), eq(asset.getId()));
                    verifyNoMoreInteractions(assetServiceMock);
                },
                EntityType.CUSTOMER, (hasId, entityCreatedMsg) -> {
                    var customer = (Customer) hasId;
                    verify(customerServiceMock).findCustomerByTenantIdAndTitleUsingCache(eq(tenantId), eq("EntityName"));
                    verify(customerServiceMock).saveCustomer(any());
                    verify(ctxMock).enqueue(eq(entityCreatedMsg), any(), any());
                    verify(customerServiceMock).findCustomerByIdAsync(eq(tenantId), eq(customer.getId()));
                    verifyNoMoreInteractions(customerServiceMock);
                }
        );
    }

    private Map<EntityType, Runnable> mockEntityServiceCallsEntityNotFound() {
        return Map.of(
                EntityType.DEVICE, () -> {
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(deviceServiceMock.findDeviceByTenantIdAndName(any(), any())).thenReturn(null);
                },
                EntityType.ASSET, () -> {
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.findAssetByTenantIdAndName(any(), any())).thenReturn(null);
                },
                EntityType.CUSTOMER, () -> {
                    when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                    when(customerServiceMock.findCustomerByTenantIdAndTitleUsingCache(any(), any())).thenReturn(null);
                },
                EntityType.ENTITY_VIEW, () -> {
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    when(entityViewServiceMock.findEntityViewByTenantIdAndName(any(), any())).thenReturn(null);
                },
                EntityType.EDGE, () -> {
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    when(edgeServiceMock.findEdgeByTenantIdAndName(any(), any())).thenReturn(null);
                },
                EntityType.USER, () -> {
                    when(ctxMock.getUserService()).thenReturn(userServiceMock);
                    when(userServiceMock.findUserByTenantIdAndEmail(any(), any())).thenReturn(null);
                },
                EntityType.DASHBOARD, () -> {
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    when(dashboardServiceMock.findFirstDashboardInfoByTenantIdAndName(any(), any())).thenReturn(null);
                }
        );
    }

    private TbMsg getTbMsg(EntityId originator, TbMsgMetaData metaData) {
        return TbMsg.newMsg(TbMsgType.NA, originator, metaData, TbMsg.EMPTY_JSON_OBJECT);
    }

    private TbMsgMetaData getMetadataWithNameTemplate() {
        var metaData = new TbMsgMetaData();
        metaData.putValue("name", "EntityName");
        return metaData;
    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // version 0 config
                Arguments.of(0,
                        "{\"direction\":\"FROM\",\"entityType\":\"DEVICE\",\"entityNamePattern\":\"$[name]\"," +
                                "\"entityTypePattern\":\"$[type]\",\"relationType\":\"Contains\"," +
                                "\"createEntityIfNotExists\":false,\"removeCurrentRelations\":false," +
                                "\"changeOriginatorToRelatedEntity\":false,\"entityCacheExpiration\":300}",
                        true,
                        "{\"direction\":\"FROM\",\"entityType\":\"DEVICE\",\"entityNamePattern\":\"$[name]\"," +
                                "\"entityTypePattern\":\"$[type]\",\"relationType\":\"Contains\"," +
                                "\"createEntityIfNotExists\":false,\"removeCurrentRelations\":false," +
                                "\"changeOriginatorToRelatedEntity\":false}"),
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"direction\":\"FROM\",\"entityType\":\"DEVICE\",\"entityNamePattern\":\"$[name]\"," +
                                "\"entityTypePattern\":\"$[type]\",\"relationType\":\"Contains\"," +
                                "\"createEntityIfNotExists\":false,\"removeCurrentRelations\":false," +
                                "\"changeOriginatorToRelatedEntity\":false}",
                        false,
                        "{\"direction\":\"FROM\",\"entityType\":\"DEVICE\",\"entityNamePattern\":\"$[name]\"," +
                                "\"entityTypePattern\":\"$[type]\",\"relationType\":\"Contains\"," +
                                "\"createEntityIfNotExists\":false,\"removeCurrentRelations\":false," +
                                "\"changeOriginatorToRelatedEntity\":false}")
        );
    }

}
