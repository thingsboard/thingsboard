// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentAppEventActionType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.HasAgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeUpgradeImageRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EDGE_OLD = "thingsboard/tb-edge-pe:3.9.0";
    private static final String EDGE_NEW = "thingsboard/tb-edge-pe:4.0.0";
    private static final String GW_OLD = "thingsboard/tb-gateway:3.6";
    private static final String GW_NEW = "thingsboard/tb-gateway:3.7";

    private final MergeUpgradeImageRule rule = new MergeUpgradeImageRule();

    // --- supports ---

    @Test
    void supports_true_forEdgeUpgrade() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AppConfigMergeCtx ctx = ctx(template, AgentAppEventActionType.UPGRADE, null);
        assertTrue(rule.supports(edgeApp(EDGE_OLD), ctx));
    }

    @Test
    void supports_false_whenActionTypeNotUpgrade() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        assertFalse(rule.supports(edgeApp(EDGE_OLD), ctx(template, AgentAppEventActionType.UPDATE, null)));
        assertFalse(rule.supports(edgeApp(EDGE_OLD), ctx(template, null, null)));
    }

    @Test
    void supports_false_whenTemplateNull() {
        assertFalse(rule.supports(edgeApp(EDGE_OLD), ctx(null, AgentAppEventActionType.UPGRADE, null)));
    }

    @Test
    void supports_false_forGenericAppType() {
        AgentAppTemplate template = template(AgentApplicationType.GENERIC, Map.of("kafka", composeWithImage(EDGE_NEW)));
        assertFalse(rule.supports(edgeApp(EDGE_OLD), ctx(template, AgentAppEventActionType.UPGRADE, null)));
    }

    // --- apply: image bump ---

    @Test
    void apply_bumpsEdgeApplicationImage() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentApplication app = edgeApp(EDGE_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(EDGE_NEW, mainImage(app));
    }

    @Test
    void apply_bumpsEdgeProfileImage() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentAppProfile profile = edgeProfile(EDGE_OLD);
        rule.apply(profile, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(EDGE_NEW, mainImage(profile));
    }

    @Test
    void apply_bumpsGatewayImage() {
        AgentAppTemplate template = template(AgentApplicationType.GATEWAY, Map.of("kafka", composeWithImage(GW_NEW)));
        AgentApplication app = app(AgentApplicationType.GATEWAY, GW_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(GW_NEW, mainImage(app));
    }

    @Test
    void apply_preservesNonMainServices() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentApplication app = edgeApp(EDGE_OLD);
        // add a sidecar the template shouldn't touch
        JsonNode compose = ((DockerComposeConfig) app.getConfig()).getCompose();
        ((ObjectNode) compose.get("services")).set("postgres", MAPPER.createObjectNode().put("image", "postgres:15"));
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(EDGE_NEW, mainImage(app));
        assertEquals("postgres:15", compose.get("services").get("postgres").get("image").asText());
    }

    // --- apply: variant selection ---

    @Test
    void apply_usesSelectedComposeTypeVariant() {
        Map<String, JsonNode> variants = new LinkedHashMap<>();
        variants.put("kafka", composeWithImage("thingsboard/tb-edge-pe:4.0.0-kafka"));
        variants.put("in_memory", composeWithImage("thingsboard/tb-edge-pe:4.0.0-mem"));
        AgentAppTemplate template = edgeTemplate(variants);
        AgentApplication app = edgeApp(EDGE_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, "in_memory"));
        assertEquals("thingsboard/tb-edge-pe:4.0.0-mem", mainImage(app));
    }

    @Test
    void apply_fallsBackToFirstVariant_whenNoComposeTypeSelected() {
        Map<String, JsonNode> variants = new LinkedHashMap<>();
        variants.put("kafka", composeWithImage("thingsboard/tb-edge-pe:4.0.0-kafka"));
        variants.put("in_memory", composeWithImage("thingsboard/tb-edge-pe:4.0.0-mem"));
        AgentAppTemplate template = edgeTemplate(variants);
        AgentApplication app = edgeApp(EDGE_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals("thingsboard/tb-edge-pe:4.0.0-kafka", mainImage(app));
    }

    /**
     * A stale compose type (renamed or dropped by a newer template version) must not silently upgrade to
     * whatever variant happens to be first.
     */
    @Test
    void apply_fallsBackToFirstVariant_whenSelectedComposeTypeIsUnknown() {
        Map<String, JsonNode> variants = new LinkedHashMap<>();
        variants.put("kafka", composeWithImage("thingsboard/tb-edge-pe:4.0.0-kafka"));
        variants.put("in_memory", composeWithImage("thingsboard/tb-edge-pe:4.0.0-mem"));
        AgentAppTemplate template = edgeTemplate(variants);
        AgentApplication app = edgeApp(EDGE_OLD);

        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, "queue_rabbitmq"));

        assertEquals("thingsboard/tb-edge-pe:4.0.0-kafka", mainImage(app));
    }

    @Test
    void apply_noOp_whenComposeTemplatesMapIsEmpty() {
        AgentAppTemplate template = edgeTemplate(Map.of());
        AgentApplication app = edgeApp(EDGE_OLD);

        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));

        assertEquals(EDGE_OLD, mainImage(app));
    }

    @Test
    void apply_noOp_whenConfigIsNotDockerCompose() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);

        assertDoesNotThrow(() -> rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null)));
        assertNull(app.getConfig());
    }

    @Test
    void apply_noOp_whenTargetComposeIsNull() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentApplication app = new AgentApplication();
        app.setAppType(AgentApplicationType.EDGE);
        app.setConfig(dockerConfig(null));

        assertDoesNotThrow(() -> rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null)));
        assertNull(((DockerComposeConfig) app.getConfig()).getCompose());
    }

    @Test
    void apply_noOp_whenNoComposeTemplateStep() {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(AgentApplicationType.EDGE);
        // no start steps -> no compose template to source the upgrade image from
        AgentApplication app = edgeApp(EDGE_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(EDGE_OLD, mainImage(app));
    }

    // --- apply: no-op cases ---

    @Test
    void apply_noOp_whenTemplateHasNoMainImage() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage("postgres:15")));
        AgentApplication app = edgeApp(EDGE_OLD);
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals(EDGE_OLD, mainImage(app));
    }

    @Test
    void apply_noOp_whenTargetImageDoesNotMatchPattern() {
        AgentAppTemplate template = edgeTemplate(Map.of("kafka", composeWithImage(EDGE_NEW)));
        AgentApplication app = edgeApp("custom/my-edge:1.0");
        rule.apply(app, ctx(template, AgentAppEventActionType.UPGRADE, null));
        assertEquals("custom/my-edge:1.0", mainImage(app));
    }

    // --- fixtures ---

    private static ObjectNode composeWithImage(String image) {
        ObjectNode service = MAPPER.createObjectNode().put("image", image);
        ObjectNode services = MAPPER.createObjectNode();
        services.set("main", service);
        ObjectNode compose = MAPPER.createObjectNode();
        compose.set("services", services);
        return compose;
    }

    private static DockerComposeConfig dockerConfig(JsonNode compose) {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }

    private static AgentAppTemplate edgeTemplate(Map<String, JsonNode> variants) {
        return template(AgentApplicationType.EDGE, variants);
    }

    private static AgentAppTemplate template(AgentApplicationType appType, Map<String, JsonNode> variants) {
        ComposeTypeChoiceStep step = new ComposeTypeChoiceStep();
        step.setComposeTemplates(new LinkedHashMap<>(variants));
        AgentAppTemplate template = new AgentAppTemplate();
        template.setAppType(appType);
        template.setStartSteps(List.<AgentAppStep>of(step));
        return template;
    }

    private static AgentApplication edgeApp(String image) {
        return app(AgentApplicationType.EDGE, image);
    }

    private static AgentApplication app(AgentApplicationType appType, String image) {
        AgentApplication app = new AgentApplication();
        app.setAppType(appType);
        app.setConfig(dockerConfig(composeWithImage(image)));
        return app;
    }

    private static AgentAppProfile edgeProfile(String image) {
        AgentAppProfile profile = new AgentAppProfile();
        profile.setAppType(AgentApplicationType.EDGE);
        profile.setConfig(dockerConfig(composeWithImage(image)));
        return profile;
    }

    private static AppConfigMergeCtx ctx(AgentAppTemplate template, AgentAppEventActionType actionType, String composeType) {
        return AppConfigMergeCtx.builder()
                .template(template)
                .actionType(actionType)
                .selectedComposeType(composeType)
                .build();
    }

    private static String mainImage(HasAgentAppConfig data) {
        JsonNode compose = ((DockerComposeConfig) data.getConfig()).getCompose();
        return compose.get("services").get("main").get("image").asText();
    }
}
