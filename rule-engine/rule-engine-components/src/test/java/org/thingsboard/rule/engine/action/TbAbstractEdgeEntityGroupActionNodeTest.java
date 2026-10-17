// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Everything TbAbstractEdgeEntityGroupActionNode does regardless of direction, run against both concrete
 * nodes. The only differences between assign and unassign are which assignment state makes the node act and
 * which service call it then makes, so those two facts are the abstract seam; subclasses keep only the tests
 * that are genuinely theirs.
 */
@ExtendWith(MockitoExtension.class)
abstract class TbAbstractEdgeEntityGroupActionNodeTest<C extends TbAbstractEdgeEntityGroupActionNodeConfiguration> {

    static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("8f88de53-4e79-4bb1-bc70-946d9a869458"));
    static final TenantId OTHER_TENANT_ID = TenantId.fromUUID(UUID.fromString("2b1c4d5e-6f70-4812-9a3b-4c5d6e7f8091"));
    static final CustomerId CUSTOMER_ID = new CustomerId(UUID.fromString("3a2f2b91-4e58-4b6c-9f4d-6b8f4e2c1a77"));
    static final EdgeId EDGE_ID = new EdgeId(UUID.fromString("7cf2a1b0-9d3e-4f5a-8b6c-1d2e3f4a5b6c"));
    static final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("6c59c85e-8351-435a-98ec-721c627b0de8"));
    static final EntityGroupId DEVICE_GROUP_ID = new EntityGroupId(UUID.fromString("bd0d1f2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f"));
    static final EntityGroupId DASHBOARD_GROUP_ID = new EntityGroupId(UUID.fromString("11112222-3333-4444-5555-666677778888"));

    final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    TbAbstractEdgeEntityGroupActionNode<C> node;

    @Mock
    TbContext ctxMock;
    @Mock
    TbPeContext peCtxMock;
    @Mock
    EntityGroupService entityGroupServiceMock;
    @Mock
    EdgeService edgeServiceMock;

    @BeforeEach
    void setUp() {
        node = newNode();
        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        lenient().when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
        lenient().when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    protected abstract TbAbstractEdgeEntityGroupActionNode<C> newNode();

    protected abstract C newConfiguration();

    /** Stub the assignment check so this node will act on the group. */
    protected abstract void stubWillApply(EntityGroupId groupId, EntityType groupType);

    /** Stub the assignment check so this node will leave the group alone. */
    protected abstract void stubWillSkip(EntityGroupId groupId, EntityType groupType);

    protected abstract void verifyApplied(EntityGroupId groupId, EntityType groupType);

    protected abstract void verifyAppliedInOrder(InOrder inOrder, EntityGroupId groupId, EntityType groupType);

    protected abstract void verifyNeverApplied();

    // ---------- init validation ----------

    @Test
    void givenNoEntityGroups_whenInit_thenThrows() {
        var configuration = newConfiguration();
        configuration.setEntityGroups(List.of());

        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("At least one entity group should be specified!");
    }

    @Test
    void givenNullEdgeSource_whenInit_thenThrows() {
        var configuration = newConfiguration();
        configuration.setEdgeSource(null);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "Site Devices")));

        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Edge source should be specified!");
    }

    @Test
    void givenBlankGroupType_whenInit_thenThrows() {
        assertThatThrownBy(() -> initWithGroups(List.of(pattern(" ", "Site Devices"))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Group type should be specified!");
    }

    @Test
    void givenBlankGroupName_whenInit_thenThrows() {
        assertThatThrownBy(() -> initWithGroups(List.of(pattern("DEVICE", " "))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Group name should be specified!");
    }

    @Test
    void givenLiteralUnsupportedGroupType_whenInit_thenThrows() {
        assertThatThrownBy(() -> initWithGroups(List.of(pattern("CUSTOMER", "Site Customers"))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Unsupported entity group type 'CUSTOMER'! Only [DEVICE, ASSET, ENTITY_VIEW, DASHBOARD, USER]"
                        + " group types are supported for edge group actions.");
    }

    @Test
    void givenLiteralGroupAllName_whenInit_thenThrows() {
        assertThatThrownBy(() -> initWithGroups(List.of(pattern("DEVICE", "All"))))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Entity group 'All' can not be used for edge group actions!");
    }

    @Test
    void givenTemplatizedGroupType_whenInit_thenSucceeds() throws TbNodeException {
        initWithGroups(List.of(pattern("${groupType}", "${groupName}")));
    }

    @Test
    void givenDataPatternTemplatizedGroupType_whenInit_thenSucceeds() throws TbNodeException {
        initWithGroups(List.of(pattern("$[groupType]", "$[groupName]")));
    }

    @Test
    void givenEdgeNamePatternSourceWithoutPattern_whenInit_thenThrows() {
        var configuration = newConfiguration();
        configuration.setEdgeSource(EdgeSource.EDGE_NAME_PATTERN);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "Site Devices")));

        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(TbNodeException.class)
                .hasMessage("Edge name pattern should be specified!");
    }

    // ---------- edge resolution ----------

    @Test
    void givenNonEdgeOriginator_whenOnMsg_thenTellFailureAndNothingApplied() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));

        node.onMsg(ctxMock, msg(DEVICE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("Message originator must be an EDGE entity, but was DEVICE!");
        verifyNeverApplied();
        verify(ctxMock, never()).tellSuccess(any());
    }

    @Test
    void givenUnknownEdgeId_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        when(edgeServiceMock.findEdgeByIdAsync(TENANT_ID, EDGE_ID)).thenReturn(immediateFuture(null));

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("Edge with id '" + EDGE_ID.getId() + "' was not found!");
        verifyNeverApplied();
    }

    @Test
    void givenEdgeOfAnotherTenant_whenOnMsg_thenTellFailureReusingNotFoundWording() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        var foreignEdge = edge();
        foreignEdge.setTenantId(OTHER_TENANT_ID);
        when(edgeServiceMock.findEdgeByIdAsync(TENANT_ID, EDGE_ID)).thenReturn(immediateFuture(foreignEdge));

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // Deliberately identical to the not-found message, so the error can not be used to probe whether
        // an edge with this id exists in some other tenant.
        assertFailedWith("Edge with id '" + EDGE_ID.getId() + "' was not found!");
        verifyNeverApplied();
    }

    @Test
    void givenEdgeNamePattern_whenOnMsg_thenResolvesEdgeFromMetadata() throws TbNodeException {
        initWithEdgeNamePattern("${siteCode}-edge", List.of(pattern("DEVICE", "Site Devices")));
        when(edgeServiceMock.findEdgeByTenantIdAndNameAsync(TENANT_ID, "plant1-edge")).thenReturn(immediateFuture(edge()));
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(DEVICE_ID, new TbMsgMetaData(Map.of("siteCode", "plant1"))));

        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenUnknownEdgeName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithEdgeNamePattern("missing-edge", List.of(pattern("DEVICE", "Site Devices")));
        when(edgeServiceMock.findEdgeByTenantIdAndNameAsync(TENANT_ID, "missing-edge")).thenReturn(immediateFuture(null));

        node.onMsg(ctxMock, msg(DEVICE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("Edge with name 'missing-edge' was not found!");
        verifyNeverApplied();
    }

    @Test
    void givenBlankResolvedEdgeName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithEdgeNamePattern("${siteCode}", List.of(pattern("DEVICE", "Site Devices")));

        node.onMsg(ctxMock, msg(DEVICE_ID, new TbMsgMetaData(Map.of("siteCode", ""))));

        assertFailedWith("Resolved edge name is empty for pattern '${siteCode}'!");
        verifyNeverApplied();
    }

    @Test
    void givenUnresolvedEdgeNamePattern_whenOnMsg_thenTellFailureWithoutEdgeLookup() throws TbNodeException {
        initWithEdgeNamePattern("${siteCode}", List.of(pattern("DEVICE", "Site Devices")));

        node.onMsg(ctxMock, msg(DEVICE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("Edge name pattern '${siteCode}' was not resolved!");
        // A missing key leaves the placeholder in place, so without the guard this would look up an edge named '${siteCode}'.
        verify(edgeServiceMock, never()).findEdgeByTenantIdAndNameAsync(any(), any());
        verifyNeverApplied();
    }

    // ---------- group resolution ----------

    @Test
    void givenGroupAtEdgeOwner_whenOnMsg_thenApplies() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenLiteralConfigAndEmptyMsgBody_whenOnMsg_thenApplies() throws TbNodeException {
        // A row with no placeholder in it must not depend on the message being parseable at all:
        // TbNodeUtils#processPattern blows up on an empty body, so the literal path has to bypass it.
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY, ""));

        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenGroupOnlyAtTenantLevel_whenOnMsg_thenWalksUpAndApplies() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Shared Devices")));
        stubEdgeById();
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, "Shared Devices"))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Shared Devices"))
                .thenReturn(Optional.of(group(DEVICE_GROUP_ID)));
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenBlankResolvedGroupName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "${groupName}")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupName", ""))));

        assertFailedWith("Resolved group name is empty for pattern '${groupName}'!");
        verifyNeverApplied();
    }

    @Test
    void givenBlankResolvedGroupType_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("${groupType}", "Site Devices")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupType", ""))));

        assertFailedWith("Resolved group type is empty for pattern '${groupType}'!");
        verifyNeverApplied();
    }

    @Test
    void givenUnresolvedPlaceholderInGroupName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "${siteCode} Devices")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("Group name pattern '${siteCode} Devices' was not resolved!");
        verifyNeverApplied();
    }

    @Test
    void givenGroupAllName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "${groupName}")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupName", "All"))));

        assertFailedWith("Entity group 'All' can not be used for edge group actions!");
        verifyNeverApplied();
    }

    @Test
    void givenEdgeGroupAllName_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "${groupName}")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupName", "[Edge] plant1-edge All"))));

        assertFailedWith("Entity group '[Edge] plant1-edge All' can not be used for edge group actions!");
        verifyNeverApplied();
    }

    @Test
    void givenUnresolvableGroup_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Site Devices"))
                .thenReturn(Optional.empty());

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        assertFailedWith("No entity group found with type 'DEVICE' and name 'Site Devices' for owner " + CUSTOMER_ID + "!");
        verifyNeverApplied();
    }

    @Test
    void givenSecondRowUnresolvable_whenOnMsg_thenNothingIsApplied() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices"), pattern("DASHBOARD", "Missing Dashboards")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, EntityType.DASHBOARD, "Missing Dashboards"))
                .thenReturn(Optional.empty());
        when(peCtxMock.getOwner(TENANT_ID, CUSTOMER_ID)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, EntityType.DASHBOARD, "Missing Dashboards"))
                .thenReturn(Optional.empty());

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // Every row resolves before anything is written, so a bad row can not half-provision the edge.
        assertFailedWith("No entity group found with type 'DASHBOARD' and name 'Missing Dashboards' for owner " + CUSTOMER_ID + "!");
        verifyNeverApplied();
    }

    // ---------- templatized group type ----------

    @Test
    void givenTemplatizedGroupType_whenOnMsg_thenResolvesFromMetadata() throws TbNodeException {
        initWithGroups(List.of(pattern("${groupType}", "Site Views")));
        stubEdgeById();
        stubGroupFound(EntityType.ENTITY_VIEW, "Site Views", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.ENTITY_VIEW);

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupType", "ENTITY_VIEW"))));

        verifyApplied(DEVICE_GROUP_ID, EntityType.ENTITY_VIEW);
    }

    @Test
    void givenTemplatizedGroupTypeWithNormalName_whenOnMsg_thenNormalisesToEntityView() throws TbNodeException {
        initWithGroups(List.of(pattern("${groupType}", "Site Views")));
        stubEdgeById();
        stubGroupFound(EntityType.ENTITY_VIEW, "Site Views", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.ENTITY_VIEW);

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupType", "entity view"))));

        verifyApplied(DEVICE_GROUP_ID, EntityType.ENTITY_VIEW);
    }

    @Test
    void givenTemplatizedGroupTypeResolvingToCustomer_whenOnMsg_thenTellFailure() throws TbNodeException {
        initWithGroups(List.of(pattern("${groupType}", "Site Customers")));
        stubEdgeById();

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("groupType", "CUSTOMER"))));

        assertFailedWith("Unsupported entity group type 'CUSTOMER' resolved from pattern '${groupType}'!"
                + " Only [DEVICE, ASSET, ENTITY_VIEW, DASHBOARD, USER] group types are supported for edge group actions.");
        verifyNeverApplied();
    }

    // ---------- apply decisions ----------

    @Test
    void givenGroupAlreadyInDesiredState_whenOnMsg_thenSkips() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillSkip(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        verifyNeverApplied();
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenTwoRowsResolvingToSameGroup_whenOnMsg_thenAppliesOnce() throws TbNodeException {
        initWithGroups(List.of(pattern("DEVICE", "Site Devices"), pattern("DEVICE", "${dup}")));
        stubEdgeById();
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, new TbMsgMetaData(Map.of("dup", "Site Devices"))));

        verifyApplied(DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    @Test
    void givenMultipleRows_whenOnMsg_thenAppliesInConfiguredRowOrder() throws TbNodeException {
        initWithGroups(List.of(pattern("DASHBOARD", "Site Dashboards"), pattern("DEVICE", "Site Devices")));
        stubEdgeById();
        stubGroupFound(EntityType.DASHBOARD, "Site Dashboards", DASHBOARD_GROUP_ID);
        stubGroupFound(EntityType.DEVICE, "Site Devices", DEVICE_GROUP_ID);
        stubWillApply(DASHBOARD_GROUP_ID, EntityType.DASHBOARD);
        stubWillApply(DEVICE_GROUP_ID, EntityType.DEVICE);

        node.onMsg(ctxMock, msg(EDGE_ID, TbMsgMetaData.EMPTY));

        // The nodeDetails promise the edge-event sequence follows configured row order.
        var inOrder = inOrder(entityGroupServiceMock);
        verifyAppliedInOrder(inOrder, DASHBOARD_GROUP_ID, EntityType.DASHBOARD);
        verifyAppliedInOrder(inOrder, DEVICE_GROUP_ID, EntityType.DEVICE);
        verify(ctxMock).tellSuccess(any());
    }

    // ---------- helpers ----------

    void initWithGroups(List<EntityGroupPattern> patterns) throws TbNodeException {
        var configuration = newConfiguration();
        configuration.setEntityGroups(patterns);
        initWithConfig(configuration);
    }

    void initWithEdgeNamePattern(String edgeNamePattern, List<EntityGroupPattern> patterns) throws TbNodeException {
        var configuration = newConfiguration();
        configuration.setEdgeSource(EdgeSource.EDGE_NAME_PATTERN);
        configuration.setEdgeNamePattern(edgeNamePattern);
        configuration.setEntityGroups(patterns);
        initWithConfig(configuration);
    }

    void initWithConfig(C configuration) throws TbNodeException {
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(configuration)));
    }

    void stubEdgeById() {
        when(edgeServiceMock.findEdgeByIdAsync(TENANT_ID, EDGE_ID)).thenReturn(immediateFuture(edge()));
    }

    void stubGroupFound(EntityType groupType, String groupName, EntityGroupId groupId) {
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, CUSTOMER_ID, groupType, groupName))
                .thenReturn(Optional.of(group(groupId)));
    }

    void assertFailedWith(String message) {
        var captor = forClass(Throwable.class);
        verify(ctxMock).tellFailure(any(), captor.capture());
        assertThat(captor.getValue()).hasMessage(message);
    }

    static EntityGroupPattern pattern(String groupType, String groupName) {
        var pattern = new EntityGroupPattern();
        pattern.setGroupType(groupType);
        pattern.setGroupName(groupName);
        return pattern;
    }

    static EntityGroup group(EntityGroupId id) {
        var group = new EntityGroup();
        group.setId(id);
        return group;
    }

    static Edge edge() {
        var edge = new Edge();
        edge.setId(EDGE_ID);
        edge.setTenantId(TENANT_ID);
        edge.setCustomerId(CUSTOMER_ID);
        edge.setName("plant1-edge");
        return edge;
    }

    static TbMsg msg(EntityId originator, TbMsgMetaData metaData) {
        return msg(originator, metaData, TbMsg.EMPTY_JSON_OBJECT);
    }

    static TbMsg msg(EntityId originator, TbMsgMetaData metaData, String data) {
        return TbMsg.newMsg()
                .type(TbMsgType.ENTITY_CREATED)
                .originator(originator)
                .copyMetaData(metaData)
                .data(data)
                .build();
    }

}
