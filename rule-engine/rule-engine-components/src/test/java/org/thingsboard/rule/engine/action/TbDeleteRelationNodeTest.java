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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
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
import org.thingsboard.server.dao.user.UserService;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbDeleteRelationNodeTest extends AbstractRuleNodeUpgradeTest {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.TENANT, EntityType.DEVICE,
            EntityType.ASSET, EntityType.CUSTOMER, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE, EntityType.USER);

    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(" ,"));

    private static final Set<EntityType> unsupportedEntityTypes = Arrays.stream(EntityType.values())
            .filter(type -> !supportedEntityTypes.contains(type)).collect(Collectors.toUnmodifiableSet());

    private static Stream<Arguments> givenSupportedEntityType_whenOnMsg_thenVerifyEntityNotFoundExceptionThrown() {
        return supportedEntityTypes.stream().filter(entityType -> !entityType.equals(EntityType.TENANT)).map(Arguments::of);
    }

    private static final TenantId tenantId = new TenantId(UUID.fromString("6fdb457d-0910-401c-8880-abc251e6a1e2"));
    private static final DeviceId deviceId = new DeviceId(UUID.fromString("4eef91a7-8865-4c3c-837d-ed6f6577508b"));
    private static final AssetId assetId = new AssetId(UUID.fromString("f4fd3b10-3f36-4d46-a162-5e62050774cc"));
    private static final CustomerId customerId = new CustomerId(UUID.fromString("ab890af2-3622-41e0-ac94-14d50af84348"));
    private static final EntityViewId entityViewId = new EntityViewId(UUID.fromString("39ce8d03-52a3-4aa8-b561-267d1d9d68b5"));
    private static final EdgeId edgeId = new EdgeId(UUID.fromString("dc4f9809-b6f9-48f9-8057-2737216cfdf7"));
    private static final DashboardId dashboardId = new DashboardId(UUID.fromString("fda72baa-c882-4723-9693-25995dc37bc5"));

    private static Stream<Arguments> givenSupportedEntityType_whenOnMsg_thenVerifyConditions() {
        return Stream.of(
                Arguments.of(new Device(deviceId)),
                Arguments.of(new Asset(assetId)),
                Arguments.of(new Customer(customerId)),
                Arguments.of(new EntityView(entityViewId)),
                Arguments.of(new Edge(edgeId)),
                Arguments.of(new Dashboard(dashboardId)),
                Arguments.of(new Tenant(tenantId))
        );
    }

    private final DeviceId originatorId = new DeviceId(UUID.fromString("574c9840-0885-4d12-be69-f557d7471a78"));

    private final ListeningExecutor dbExecutor = new TestDbCallbackExecutor();

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
    private RelationService relationServiceMock;


    private TbDeleteRelationNode node;
    private TbDeleteRelationNodeConfiguration config;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = spy(new TbDeleteRelationNode());
        config = new TbDeleteRelationNodeConfiguration().defaultConfiguration();
    }

    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        var defaultConfig = new TbDeleteRelationNodeConfiguration().defaultConfiguration();
        assertThat(defaultConfig.getDirection()).isEqualTo(EntitySearchDirection.FROM);
        assertThat(defaultConfig.getRelationType()).isEqualTo(EntityRelation.CONTAINS_TYPE);
        assertThat(defaultConfig.getEntityNamePattern()).isEqualTo("");
        assertThat(defaultConfig.getEntityTypePattern()).isEqualTo(null);
        assertThat(defaultConfig.getEntityType()).isEqualTo(null);
        assertThat(defaultConfig.isDeleteForSingleEntity()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EntityType.class)
    void givenEntityType_whenInit_thenVerifyExceptionThrownIfTypeIsUnsupported(EntityType entityType) {
        // GIVEN
        config.setEntityType(entityType);
        config.setDeleteForSingleEntity(true);
        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN-THEN
        if (unsupportedEntityTypes.contains(entityType)) {
            assertThatThrownBy(() -> node.init(ctxMock, nodeConfiguration))
                    .isInstanceOf(TbNodeException.class)
                    .hasMessage("Unsupported entity type '" + entityType +
                            "'! Only " + supportedEntityTypesStr + " types are allowed.");
        } else {
            assertThatCode(() -> node.init(ctxMock, nodeConfiguration)).doesNotThrowAnyException();
        }
        verifyNoInteractions(ctxMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyEntityNotFoundExceptionThrown")
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsTrue_thenVerifyEntityNotFoundExceptionThrown(EntityType entityType) throws TbNodeException {
        // GIVEN
        config.setEntityType(entityType);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setDeleteForSingleEntity(true);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);

        var mockMethodCallsMap = mockEntityServiceCallsEntityNotFound();
        mockMethodCallsMap.get(entityType).run();

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        node.onMsg(ctxMock, msg);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        String validationField = switch (entityType) {
            case CUSTOMER, DASHBOARD -> "title";
            case USER -> "email";
            default -> "name";
        };
        assertThat(throwableCaptor.getValue())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("%s with %s 'EntityName' doesn't exist!", entityType.getNormalName(), validationField);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsTrue_thenVerifyRelationDeletedAndOutMsgSuccess(HasId entity) throws TbNodeException {
        // GIVEN
        var entityId = (EntityId) entity.getId();
        var entityType = entityId.getEntityType();

        config.setEntityType(entityType);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setDeleteForSingleEntity(true);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        if (entityType.equals(EntityType.TENANT)) {
            when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        }
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCalls();
        mockMethodCallsMap.get(entityType).accept(entity);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationServiceMock.deleteRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCalls();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(originatorId), eq(entityId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).deleteRelationAsync(eq(tenantId), eq(originatorId), eq(entityId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));

        verify(ctxMock).tellSuccess(eq(msg));
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());
        if (entityType.equals(EntityType.TENANT)) {
            verify(ctxMock).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsTrue_thenVerifyRelationFailedToDeleteAndOutMsgFailure(HasId entity) throws TbNodeException {
        // GIVEN
        var entityId = (EntityId) entity.getId();
        var entityType = entityId.getEntityType();

        config.setEntityType(entityType);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setDeleteForSingleEntity(true);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        if (entityType.equals(EntityType.TENANT)) {
            when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        }
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCalls();
        mockMethodCallsMap.get(entityType).accept(entity);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(true));
        when(relationServiceMock.deleteRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCalls();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(originatorId), eq(entityId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).deleteRelationAsync(eq(tenantId), eq(originatorId), eq(entityId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));

        var throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellSuccess(any());
        if (entityType.equals(EntityType.TENANT)) {
            verify(ctxMock).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage("Failed to delete relation(s) with originator!");
    }

    @ParameterizedTest
    @MethodSource("givenSupportedEntityType_whenOnMsg_thenVerifyConditions")
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsTrue_thenVerifyRelationNotFoundAndOutMsgSuccess(HasId entity) throws TbNodeException {
        // GIVEN
        var entityId = (EntityId) entity.getId();
        var entityType = entityId.getEntityType();

        config.setEntityType(entityType);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");
        config.setDeleteForSingleEntity(true);

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        if (entityType.equals(EntityType.TENANT)) {
            when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);
        }
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);

        var mockMethodCallsMap = mockEntityServiceCalls();
        mockMethodCallsMap.get(entityType).accept(entity);

        when(relationServiceMock.checkRelationAsync(any(), any(), any(), any(), any())).thenReturn(Futures.immediateFuture(false));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var verifyMethodCallsMap = verifyEntityServiceCalls();
        verifyMethodCallsMap.get(entityType).accept(entity);

        verify(relationServiceMock).checkRelationAsync(eq(tenantId), eq(originatorId), eq(entityId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));

        verify(ctxMock).tellSuccess(eq(msg));
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());
        if (entityType.equals(EntityType.TENANT)) {
            verify(ctxMock).getDbCallbackExecutor();
        }
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }

    @Test
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsFalse_thenVerifyRelationsDeletedAndOutMsgSuccess() throws TbNodeException {
        // GIVEN

        config.setEntityType(EntityType.DEVICE);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        var relationToDelete = new EntityRelation();
        when(relationServiceMock.findByFromAndTypeAsync(any(), any(), any(), any())).thenReturn(Futures.immediateFuture(List.of(relationToDelete)));
        when(relationServiceMock.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(true));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        verify(relationServiceMock).findByFromAndTypeAsync(eq(tenantId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).deleteRelationAsync(eq(tenantId), eq(relationToDelete));

        verify(ctxMock).tellSuccess(eq(msg));
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellFailure(any(), any());
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
    }


    @Test
    void givenSupportedEntityType_whenOnMsgAndDeleteForSingleEntityIsFalse_thenVerifyRelationFailedToDeleteAndOutMsgFailure() throws TbNodeException {
        // GIVEN
        config.setEntityType(EntityType.DEVICE);
        config.setEntityNamePattern("${name}");
        config.setEntityTypePattern("${type}");

        var nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);

        when(ctxMock.getTenantId()).thenReturn(tenantId);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbExecutor);

        var relationToDelete = new EntityRelation();
        when(relationServiceMock.findByFromAndTypeAsync(any(), any(), any(), any())).thenReturn(Futures.immediateFuture(List.of(relationToDelete)));
        when(relationServiceMock.deleteRelationAsync(any(), any())).thenReturn(Futures.immediateFuture(false));

        var md = getMetadataWithNameTemplate();
        var msg = getTbMsg(originatorId, md);

        // WHEN
        node.onMsg(ctxMock, msg);

        verify(relationServiceMock).findByFromAndTypeAsync(eq(tenantId), eq(originatorId), eq(EntityRelation.CONTAINS_TYPE), eq(RelationTypeGroup.COMMON));
        verify(relationServiceMock).deleteRelationAsync(eq(tenantId), eq(relationToDelete));

        var throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());
        verify(ctxMock, never()).tellNext(any(), anyString());
        verify(ctxMock, never()).tellNext(any(), anySet());
        verify(ctxMock, never()).tellSuccess(any());
        verifyNoMoreInteractions(ctxMock, relationServiceMock);
        assertThat(throwableCaptor.getValue()).isInstanceOf(RuntimeException.class).hasMessage("Failed to delete relation(s) with originator!");
    }


    private Map<EntityType, Runnable> mockEntityServiceCallsEntityNotFound() {
        return Map.of(
                EntityType.DEVICE, () -> {
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(deviceServiceMock.findDeviceByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                },
                EntityType.ASSET, () -> {
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.findAssetByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                },
                EntityType.CUSTOMER, () -> {
                    when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                    when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.empty()));
                },
                EntityType.ENTITY_VIEW, () -> {
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    when(entityViewServiceMock.findEntityViewByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                },
                EntityType.EDGE, () -> {
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    when(edgeServiceMock.findEdgeByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                },
                EntityType.USER, () -> {
                    when(ctxMock.getUserService()).thenReturn(userServiceMock);
                    when(userServiceMock.findUserByTenantIdAndEmailAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                },
                EntityType.DASHBOARD, () -> {
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    when(dashboardServiceMock.findFirstDashboardInfoByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(null));
                }
        );
    }

    private Map<EntityType, Consumer<HasId>> mockEntityServiceCalls() {
        return Map.of(
                EntityType.DEVICE, hasId -> {
                    var device = (Device) hasId;
                    when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
                    when(deviceServiceMock.findDeviceByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(device));
                },
                EntityType.ASSET, hasId -> {
                    var asset = (Asset) hasId;
                    when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
                    when(assetServiceMock.findAssetByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(asset));
                },
                EntityType.CUSTOMER, hasId -> {
                    var customer = (Customer) hasId;
                    when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
                    when(customerServiceMock.findCustomerByTenantIdAndTitleAsync(any(), any())).thenReturn(Futures.immediateFuture(Optional.ofNullable(customer)));
                },
                EntityType.ENTITY_VIEW, hasId -> {
                    var entityView = (EntityView) hasId;
                    when(ctxMock.getEntityViewService()).thenReturn(entityViewServiceMock);
                    when(entityViewServiceMock.findEntityViewByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(entityView));
                },
                EntityType.EDGE, hasId -> {
                    var edge = (Edge) hasId;
                    when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
                    when(edgeServiceMock.findEdgeByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(edge));
                },
                EntityType.USER, hasId -> {
                    var user = (User) hasId;
                    when(ctxMock.getUserService()).thenReturn(userServiceMock);
                    when(userServiceMock.findUserByTenantIdAndEmailAsync(any(), any())).thenReturn(Futures.immediateFuture(user));
                },
                EntityType.DASHBOARD, hasId -> {
                    var dashboard = (Dashboard) hasId;
                    when(ctxMock.getDashboardService()).thenReturn(dashboardServiceMock);
                    when(dashboardServiceMock.findFirstDashboardInfoByTenantIdAndNameAsync(any(), any())).thenReturn(Futures.immediateFuture(dashboard));
                },
                EntityType.TENANT, hasId -> {
                    // do nothing. tenantId returned by ctxMock.
                }
        );
    }

    private Map<EntityType, Consumer<HasId>> verifyEntityServiceCalls() {
        return Map.of(
                EntityType.DEVICE, hasId -> {
                    verify(deviceServiceMock).findDeviceByTenantIdAndNameAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(deviceServiceMock);
                },
                EntityType.ASSET, hasId -> {
                    verify(assetServiceMock).findAssetByTenantIdAndNameAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(assetServiceMock);
                },
                EntityType.CUSTOMER, hasId -> {
                    verify(customerServiceMock).findCustomerByTenantIdAndTitleAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(customerServiceMock);
                },
                EntityType.ENTITY_VIEW, hasId -> {
                    verify(entityViewServiceMock).findEntityViewByTenantIdAndNameAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(entityViewServiceMock);
                },
                EntityType.EDGE, hasId -> {
                    verify(edgeServiceMock).findEdgeByTenantIdAndNameAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(edgeServiceMock);
                },
                EntityType.USER, hasId -> {
                    verify(userServiceMock).findUserByTenantIdAndEmailAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(userServiceMock);
                },
                EntityType.DASHBOARD, hasId -> {
                    verify(dashboardServiceMock).findFirstDashboardInfoByTenantIdAndNameAsync(eq(tenantId), eq("EntityName"));
                    verifyNoMoreInteractions(dashboardServiceMock);
                },
                EntityType.TENANT, hasId -> {
                }
        );
    }

    private TbMsg getTbMsg(EntityId originator, TbMsgMetaData metaData) {
        return TbMsg.newMsg()
                .type(TbMsgType.NA)
                .originator(originator)
                .copyMetaData(metaData)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
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
                // version 0 config, FROM direction.
                Arguments.of(0,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\",\"entityCacheExpiration\":300}",
                        true,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"TO\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}"),
                // version 0 config, TO direction.
                Arguments.of(0,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"TO\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\",\"entityCacheExpiration\":300}",
                        true,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}"),
                // config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}",
                        false,
                        "{\"deleteForSingleEntity\":true,\"direction\":\"FROM\",\"entityType\":\"DEVICE\"," +
                                "\"entityNamePattern\":\"$[name]\",\"relationType\":\"Contains\"}")
        );
    }

}
