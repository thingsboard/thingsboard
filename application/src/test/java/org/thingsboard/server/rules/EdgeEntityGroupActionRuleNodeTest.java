// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.rule.engine.action.EdgeSource;
import org.thingsboard.rule.engine.action.EntityGroupPattern;
import org.thingsboard.rule.engine.action.TbAbstractEdgeEntityGroupActionNodeConfiguration;
import org.thingsboard.rule.engine.action.TbAssignEntityGroupToEdgeNode;
import org.thingsboard.rule.engine.action.TbAssignEntityGroupToEdgeNodeConfiguration;
import org.thingsboard.rule.engine.action.TbUnassignEntityGroupFromEdgeNode;
import org.thingsboard.rule.engine.action.TbUnassignEntityGroupFromEdgeNodeConfiguration;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNode;
import org.thingsboard.rule.engine.debug.TbMsgGeneratorNodeConfiguration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.controller.AbstractRuleEngineControllerTest;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "edges.enabled=true",
        "edges.rpc.port=0"
})
@DaoSqlTest
public class EdgeEntityGroupActionRuleNodeTest extends AbstractRuleEngineControllerTest {

    private Edge edge;
    private EntityGroup deviceGroup;

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
        edge = doPost("/api/edge", constructEdge("Edge group actions", "default"), Edge.class);

        deviceGroup = createDeviceGroup("Site Devices");
    }

    @Test
    public void givenBothNodes_whenListingActionComponents_thenBothAreDiscoverable() throws Exception {
        List<ComponentDescriptor> actionNodes = readResponse(
                doGet("/api/components?componentTypes={componentTypes}&ruleChainType={ruleChainType}",
                        ComponentType.ACTION, RuleChainType.CORE).andExpect(status().isOk()),
                new TypeReference<List<ComponentDescriptor>>() {});

        // A node missing here cannot be dropped into a rule chain at all, whatever its own tests say.
        assertThat(actionNodes).extracting(ComponentDescriptor::getClazz)
                .contains(TbAssignEntityGroupToEdgeNode.class.getName(),
                        TbUnassignEntityGroupFromEdgeNode.class.getName());
    }

    @Test
    public void givenLiteralGroupName_whenAssignNodeRuns_thenGroupIsAssignedToEdge() throws Exception {
        var configuration = new TbAssignEntityGroupToEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        configuration.setEntityGroups(List.of(pattern("DEVICE", deviceGroup.getName())));
        configuration.setCreateGroupIfNotExists(false);

        assertThat(assignedDeviceGroupNames()).isEmpty();

        runChain(TbAssignEntityGroupToEdgeNode.class, configuration, "{}");

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedDeviceGroupNames().contains(deviceGroup.getName()));
    }

    @Test
    public void givenTemplatizedGroupName_whenUnassignNodeRuns_thenGroupIsUnassignedFromEdge() throws Exception {
        doPost("/api/edge/" + edge.getId().getId() + "/entityGroup/" + deviceGroup.getId().getId() + "/DEVICE",
                EntityGroup.class);
        assertThat(assignedDeviceGroupNames()).containsExactly(deviceGroup.getName());

        var configuration = new TbUnassignEntityGroupFromEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "${groupName}")));

        runChain(TbUnassignEntityGroupFromEdgeNode.class, configuration,
                "{ groupName: \"" + deviceGroup.getName() + "\" }");

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedDeviceGroupNames().isEmpty());
    }

    @Test
    public void givenCreateGroupIfNotExists_whenAssignNodeRuns_thenGroupIsCreatedUnderEdgeOwnerAndAssigned() throws Exception {
        var configuration = new TbAssignEntityGroupToEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        configuration.setEntityGroups(List.of(pattern("DEVICE", "Provisioned Devices")));
        configuration.setCreateGroupIfNotExists(true);

        assertThat(tenantDeviceGroupNames()).doesNotContain("Provisioned Devices");

        runChain(TbAssignEntityGroupToEdgeNode.class, configuration, "{}");

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedDeviceGroupNames().contains("Provisioned Devices"));
        // This edge is tenant-owned, so landing "under the edge owner" means showing up at tenant level.
        assertThat(tenantDeviceGroupNames()).contains("Provisioned Devices");
    }

    @Test
    public void givenEdgeNamePattern_whenAssignNodeRuns_thenResolvesEdgeFromMetadataAndAssigns() throws Exception {
        Device device = new Device();
        device.setName("Provisioning source");
        device.setType("default");
        device = doPost("/api/device", device, Device.class);

        var configuration = new TbAssignEntityGroupToEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.EDGE_NAME_PATTERN);
        configuration.setEdgeNamePattern("${edgeName}");
        configuration.setEntityGroups(List.of(pattern("DEVICE", deviceGroup.getName())));
        configuration.setCreateGroupIfNotExists(false);

        assertThat(assignedDeviceGroupNames()).isEmpty();

        // Originating from a device rather than the edge, so the edge can only come from the pattern.
        runChain(TbAssignEntityGroupToEdgeNode.class, configuration,
                "{ edgeName: \"" + edge.getName() + "\" }", device.getId());

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedDeviceGroupNames().contains(deviceGroup.getName()));
    }

    @Test
    public void givenGroupAlreadyAssigned_whenAssignNodeRuns_thenNoSecondAssignedToEdgeEvent() throws Exception {
        activateEdge();
        EntityGroup otherGroup = createDeviceGroup("Other Devices");
        doPost("/api/edge/" + edge.getId().getId() + "/entityGroup/" + deviceGroup.getId().getId() + "/DEVICE",
                EntityGroup.class);
        // Also proves edge events are recorded in this context, so the assertion below cannot pass vacuously.
        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedToEdgeEvents(deviceGroup.getId().getId()) == 1);

        var configuration = new TbAssignEntityGroupToEdgeNodeConfiguration();
        configuration.setEdgeSource(EdgeSource.MESSAGE_ORIGINATOR);
        // Already-assigned row first: any redundant event for it would be published before the one gating the assertion.
        configuration.setEntityGroups(List.of(pattern("DEVICE", deviceGroup.getName()),
                pattern("DEVICE", otherGroup.getName())));
        configuration.setCreateGroupIfNotExists(false);

        runChain(TbAssignEntityGroupToEdgeNode.class, configuration, "{}");

        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> assignedToEdgeEvents(otherGroup.getId().getId()) == 1);
        // A redundant assign re-publishes ASSIGNED_TO_EDGE, making the edge re-sync every entity in the group.
        assertThat(assignedToEdgeEvents(deviceGroup.getId().getId())).isEqualTo(1);
    }

    private void runChain(Class<?> nodeClazz, TbAbstractEdgeEntityGroupActionNodeConfiguration configuration,
                          String generatedMetadata) throws Exception {
        runChain(nodeClazz, configuration, generatedMetadata, edge.getId());
    }

    private void runChain(Class<?> nodeClazz, TbAbstractEdgeEntityGroupActionNodeConfiguration configuration,
                          String generatedMetadata, EntityId originator) throws Exception {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(nodeClazz.getSimpleName() + " chain");
        ruleChain.setTenantId(tenantId);
        ruleChain.setDebugMode(true);
        ruleChain = saveRuleChain(ruleChain);

        RuleChainMetaData metaData = new RuleChainMetaData();
        metaData.setRuleChainId(ruleChain.getId());
        metaData.setNodes(List.of(generatorNode(generatedMetadata, originator), actionNode(nodeClazz, configuration)));
        metaData.setFirstNodeIndex(0);
        metaData.addConnectionInfo(0, 1, TbNodeConnectionType.SUCCESS);
        saveRuleChainMetaData(metaData);
    }

    private RuleNode generatorNode(String generatedMetadata, EntityId originator) {
        RuleNode node = new RuleNode();
        node.setName("Generator");
        node.setType(TbMsgGeneratorNode.class.getName());
        node.setConfigurationVersion(configurationVersion(TbMsgGeneratorNode.class));
        node.setDebugSettings(DebugSettings.all());

        var configuration = new TbMsgGeneratorNodeConfiguration();
        configuration.setMsgCount(1);
        configuration.setPeriodInSeconds(1);
        configuration.setOriginatorId(originator.getId().toString());
        configuration.setOriginatorType(originator.getEntityType());
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setTbelScript("""
                var msg = {};
                var metadata = %s;
                var msgType = "POST_TELEMETRY_REQUEST";
                return { msg: msg, metadata: metadata, msgType: msgType };
                """.formatted(generatedMetadata));
        node.setConfiguration(JacksonUtil.valueToTree(configuration));
        return node;
    }

    private RuleNode actionNode(Class<?> nodeClazz, TbAbstractEdgeEntityGroupActionNodeConfiguration configuration) {
        RuleNode node = new RuleNode();
        node.setName(nodeClazz.getSimpleName());
        node.setType(nodeClazz.getName());
        node.setConfigurationVersion(configurationVersion(nodeClazz));
        node.setDebugSettings(DebugSettings.all());
        node.setConfiguration(JacksonUtil.valueToTree(configuration));
        return node;
    }

    private int configurationVersion(Class<?> nodeClazz) {
        return nodeClazz.getAnnotation(org.thingsboard.rule.engine.api.RuleNode.class).version();
    }

    private List<String> assignedDeviceGroupNames() throws Exception {
        PageData<EntityGroupInfo> groups = doGetTypedWithPageLink(
                "/api/entityGroups/edge/" + edge.getId().getId() + "/DEVICE?",
                new TypeReference<PageData<EntityGroupInfo>>() {},
                new PageLink(100));
        return groups.getData().stream()
                .map(EntityGroup::getName)
                // Every edge carries a platform-managed "[Edge] All" group that the nodes must never touch.
                .filter(name -> !name.startsWith("[Edge]"))
                .toList();
    }

    private List<String> tenantDeviceGroupNames() throws Exception {
        PageData<EntityGroupInfo> groups = doGetTypedWithPageLink(
                "/api/entityGroups/TENANT/" + tenantId.getId() + "/DEVICE?",
                new TypeReference<PageData<EntityGroupInfo>>() {},
                new PageLink(100));
        return groups.getData().stream().map(EntityGroup::getName).toList();
    }

    private long assignedToEdgeEvents(UUID entityGroupId) throws Exception {
        PageData<EdgeEvent> events = doGetTypedWithTimePageLink(
                "/api/edge/" + edge.getId().getId() + "/events?",
                new TypeReference<PageData<EdgeEvent>>() {},
                new TimePageLink(1000));
        return events.getData().stream()
                .filter(event -> EdgeEventType.ENTITY_GROUP.equals(event.getType()))
                .filter(event -> EdgeEventActionType.ASSIGNED_TO_EDGE.equals(event.getAction()))
                .filter(event -> entityGroupId.equals(event.getEntityId()))
                .count();
    }

    private void activateEdge() throws Exception {
        var attributes = JacksonUtil.newObjectNode();
        attributes.put("active", true);
        doPost("/api/plugins/telemetry/EDGE/" + edge.getId() + "/attributes/" + AttributeScope.SERVER_SCOPE, attributes);
    }

    private EntityGroup createDeviceGroup(String name) throws Exception {
        EntityGroup group = new EntityGroup();
        group.setName(name);
        group.setType(EntityType.DEVICE);
        group.setOwnerId(tenantId);
        return doPost("/api/entityGroup", group, EntityGroup.class);
    }

    private static EntityGroupPattern pattern(String groupType, String groupName) {
        var pattern = new EntityGroupPattern();
        pattern.setGroupType(groupType);
        pattern.setGroupName(groupName);
        return pattern;
    }

}
