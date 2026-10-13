// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AppConfigMergeCtx;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeStartStep;
import org.thingsboard.server.common.data.agent.step.ComposeStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.dao.agent.config.MergeTemplateComposeRule;
import org.thingsboard.server.exception.ThingsboardRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeTemplateComposeRuleTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MergeTemplateComposeRule rule;

    @BeforeEach
    void setUp() {
        rule = new MergeTemplateComposeRule();
    }

    // ==================== supports() tests ====================

    @Test
    void supports_shouldReturnFalse_whenCtxCarriesNoTemplate() {
        AgentApplication app = createAppWithCompose(List.of(), null);

        assertFalse(rule.supports(app, AppConfigMergeCtx.empty()));
    }

    @Test
    void supports_shouldReturnFalse_whenCtxIsNull() {
        AgentApplication app = createAppWithCompose(List.of(), null);

        assertFalse(rule.supports(app, null));
    }

    /**
     * supports() only gates on the template and the selected compose type - the choice-step lookup lives in
     * apply(), which no-ops (see apply_shouldDoNothing_whenTemplateHasNoComposeTypeChoiceStep).
     */
    @Test
    void supports_shouldReturnTrue_whenTemplateHasNoComposeTypeChoiceStep() {
        AgentApplication app = createAppWithCompose(List.of(), null);
        AgentAppTemplate template = new AgentAppTemplate();
        template.setStartSteps(List.of(createNonComposeStep()));
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType("monolith").build();

        assertTrue(rule.supports(app, ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenSelectedComposeTypeIsNull() {
        ComposeTypeChoiceStep choiceStep = createChoiceStep(
                Map.of("monolith", MAPPER.createObjectNode()));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType(null).build();

        assertFalse(rule.supports(createAppWithCompose(List.of(), null), ctx));
    }

    @Test
    void supports_shouldReturnFalse_whenSelectedComposeTypeIsEmpty() {
        ComposeTypeChoiceStep choiceStep = createChoiceStep(
                Map.of("monolith", MAPPER.createObjectNode()));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType("").build();

        assertFalse(rule.supports(createAppWithCompose(List.of(), null), ctx));
    }

    @Test
    void supports_shouldReturnTrue_whenSelectedComposeTypeIsPresent() {
        ComposeTypeChoiceStep choiceStep = createChoiceStep(
                Map.of("monolith", MAPPER.createObjectNode()));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType("monolith").build();

        assertTrue(rule.supports(createAppWithCompose(List.of(), null), ctx));
    }

    // ==================== apply() - skip scenarios ====================

    @Test
    void apply_shouldDoNothing_whenTemplateHasNoComposeTypeChoiceStep() {
        ComposeStep composeStep = createComposeStep();
        JsonNode compose = MAPPER.createObjectNode().put("service", "value");

        AgentApplication app = createAppWithCompose(List.of(composeStep), compose);
        AgentAppTemplate template = new AgentAppTemplate();
        template.setStartSteps(List.of(createNonComposeStep()));
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType("monolith").build();

        rule.apply(app, ctx);

        assertEquals("value", getAppCompose(app).get("service").asText());
    }

    @Test
    void apply_shouldDoNothing_whenTemplateInstallStepsAreNull() {
        ComposeStep composeStep = createComposeStep();
        JsonNode compose = MAPPER.createObjectNode().put("key", "val");

        AgentApplication app = createAppWithCompose(List.of(composeStep), compose);
        AgentAppTemplate template = new AgentAppTemplate();
        template.setStartSteps(null);
        AppConfigMergeCtx ctx = AppConfigMergeCtx.builder().template(template).selectedComposeType("monolith").build();

        rule.apply(app, ctx);

        assertEquals("val", getAppCompose(app).get("key").asText());
    }

    // ==================== apply() - null/empty appCompose ====================

    @Test
    void apply_shouldSetComposeFromTemplate_whenAppComposeIsNull() {
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("tb-core", "image:core")
                .put("tb-rule", "image:rule");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), null);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertNotNull(result);
        assertEquals("image:core", result.get("tb-core").asText());
        assertEquals("image:rule", result.get("tb-rule").asText());
    }

    @Test
    void apply_shouldSetComposeFromTemplate_whenAppComposeIsNullNode() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("svc", "img");
        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), MAPPER.nullNode());
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        assertEquals("img", getAppCompose(app).get("svc").asText());
    }

    // ==================== apply() - deep merge: add keys ====================

    @Test
    void apply_shouldOverwriteWithTemplateValues_andAddNewKeys() {
        // Template-wins: existing keys take the template value and new keys
        // declared by the template are added.
        ObjectNode appCompose = MAPPER.createObjectNode().put("existing", "value");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("existing", "template-value")
                .put("newKey", "new-value");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertEquals("template-value", result.get("existing").asText());
        assertEquals("new-value", result.get("newKey").asText());
    }

    // ==================== apply() - deep merge: preserve user keys ====================

    @Test
    void apply_shouldPreserveTopLevelKeysNotInTemplate() {
        // App-only keys at the top level are kept; keys also declared by the
        // template take the template value.
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("keep", "kept-value")
                .set("userKey", MAPPER.createObjectNode()
                        .put("k1", "v1"));
        ObjectNode templateCompose = MAPPER.createObjectNode().put("keep", "template-value");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertEquals("template-value", result.get("keep").asText());
        assertNotNull(result.get("userKey"));
        assertEquals("v1", result.get("userKey").get("k1").asText());
    }

    // ==================== apply() - deep merge: preserve values ====================

    @Test
    void apply_shouldOverwriteLeafValues_whenBothDeclareTheKey() {
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("port", 9090)
                .put("host", "custom-host");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("port", 8080)
                .put("host", "default-host");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertEquals(8080, result.get("port").asInt());
        assertEquals("default-host", result.get("host").asText());
    }

    // ==================== apply() - deep merge: nested objects ====================

    @Test
    void apply_shouldReplaceNestedObjectWholesale_whenBothDeclareKey() {
        // No deep recursion: the template's "service" object replaces the
        // app's "service" object entirely.
        ObjectNode appNested = MAPPER.createObjectNode().put("existingProp", "custom");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("service", appNested);

        ObjectNode templateNested = MAPPER.createObjectNode()
                .put("existingProp", "default")
                .put("newProp", "added");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("service", templateNested);

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode resultService = getAppCompose(app).get("service");
        assertEquals("default", resultService.get("existingProp").asText());
        assertEquals("added", resultService.get("newProp").asText());
    }

    @Test
    void apply_shouldDropAppOnlyNestedKeys_whenTemplateDeclaresParent() {
        // Wholesale replacement means app-only nested keys are dropped if the
        // template also declares the parent object.
        ObjectNode appNested = MAPPER.createObjectNode()
                .put("keep", "val")
                .put("userCustom", "preserved");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("service", appNested);

        ObjectNode templateNested = MAPPER.createObjectNode().put("keep", "default");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("service", templateNested);

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode resultService = getAppCompose(app).get("service");
        assertEquals("default", resultService.get("keep").asText());
        assertNull(resultService.get("userCustom"));
    }

    @Test
    void apply_shouldReplaceTopLevelObject_andLoseDeeperAppOnlyKeys() {
        // app: { a: { b: { existing: "custom", userProp: "keep" } } }
        ObjectNode appLevel3 = MAPPER.createObjectNode()
                .put("existing", "custom")
                .put("userProp", "keep");
        ObjectNode appLevel2 = MAPPER.createObjectNode();
        appLevel2.set("b", appLevel3);
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("a", appLevel2);

        // template: { a: { b: { existing: "default", added: "new" } } }
        ObjectNode tplLevel3 = MAPPER.createObjectNode()
                .put("existing", "default")
                .put("added", "new");
        ObjectNode tplLevel2 = MAPPER.createObjectNode();
        tplLevel2.set("b", tplLevel3);
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("a", tplLevel2);

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app).get("a").get("b");
        assertEquals("default", result.get("existing").asText());
        assertEquals("new", result.get("added").asText());
        assertNull(result.get("userProp"));
    }

    // ==================== apply() - deep merge: type mismatches ====================

    @Test
    void apply_shouldOverwriteAppLeaf_whenTemplateHasObject() {
        // app has a leaf, template has an object at the same key — template wins.
        ObjectNode appCompose = MAPPER.createObjectNode().put("config", "flat-string");
        ObjectNode templateNested = MAPPER.createObjectNode().put("nested", "value");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("config", templateNested);

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode resultConfig = getAppCompose(app).get("config");
        assertTrue(resultConfig.isObject());
        assertEquals("value", resultConfig.get("nested").asText());
    }

    @Test
    void apply_shouldOverwriteAppObject_whenTemplateHasLeaf() {
        // app has an object, template has a leaf at the same key — template wins.
        ObjectNode appNested = MAPPER.createObjectNode().put("inner", "val");
        ObjectNode appCompose = MAPPER.createObjectNode();
        appCompose.set("config", appNested);

        ObjectNode templateCompose = MAPPER.createObjectNode().put("config", "flat");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        assertEquals("flat", getAppCompose(app).get("config").asText());
    }

    // ==================== apply() - deep merge: deep copy isolation ====================

    @Test
    void apply_shouldDeepCopyAddedKeys_soTemplateIsNotMutated() {
        ObjectNode templateNested = MAPPER.createObjectNode().put("prop", "original");
        ObjectNode templateCompose = MAPPER.createObjectNode();
        templateCompose.set("newService", templateNested);

        ObjectNode appCompose = MAPPER.createObjectNode();
        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        // Mutate the result
        ((ObjectNode) getAppCompose(app).get("newService")).put("prop", "mutated");

        // Template should be unaffected
        assertEquals("original", templateNested.get("prop").asText());
    }

    @Test
    void apply_shouldDeepCopyFullTemplate_whenAppComposeIsNull() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("key", "original");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), null);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        ((ObjectNode) getAppCompose(app)).put("key", "mutated");
        assertEquals("original", templateCompose.get("key").asText());
    }

    // ==================== apply() - error scenarios ====================

    @Test
    void apply_shouldThrow_whenSelectedComposeTypeNotInTemplate() {
        ObjectNode templateCompose = MAPPER.createObjectNode().put("svc", "val");
        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), MAPPER.createObjectNode());
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"nonexistent-type");

        ThingsboardRuntimeException ex = assertThrows(ThingsboardRuntimeException.class,
                () -> rule.apply(app, ctx));

        assertEquals(ThingsboardErrorCode.BAD_REQUEST_PARAMS, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("nonexistent-type"));
    }

    // ==================== apply() - combined add and preserve ====================

    @Test
    void apply_shouldAddNewTemplateKeysAndOverwriteSharedKeys() {
        // Template-wins: shared keys take template values; template-only keys
        // are added; app-only keys at the same level are preserved.
        ObjectNode appCompose = MAPPER.createObjectNode()
                .put("keep", "app-val")
                .put("userKey1", "user1")
                .put("userKey2", "user2");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("keep", "tpl-val")
                .put("add1", "new1")
                .put("add2", "new2");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));

        AgentApplication app = createAppWithCompose(List.of(composeStep), appCompose);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"monolith");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertEquals(5, result.size());
        assertEquals("tpl-val", result.get("keep").asText());
        assertEquals("new1", result.get("add1").asText());
        assertEquals("new2", result.get("add2").asText());
        assertEquals("user1", result.get("userKey1").asText());
        assertEquals("user2", result.get("userKey2").asText());
    }

    // ==================== apply() - compose type selection ====================

    @Test
    void apply_shouldSelectCorrectComposeTypeFromChoiceStep() {
        ObjectNode monolithCompose = MAPPER.createObjectNode().put("all-in-one", "img");
        ObjectNode microservicesCompose = MAPPER.createObjectNode()
                .put("core", "core-img")
                .put("rule", "rule-img");

        ComposeStep composeStep = createComposeStep();
        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of(
                "monolith", monolithCompose,
                "microservices", microservicesCompose
        ));

        AgentApplication app = createAppWithCompose(List.of(composeStep), null);
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template,"microservices");

        rule.apply(app, ctx);

        JsonNode result = getAppCompose(app);
        assertEquals("core-img", result.get("core").asText());
        assertEquals("rule-img", result.get("rule").asText());
        assertNull(result.get("all-in-one"));
    }

    // ==================== AgentAppProfile tests ====================

    @Test
    void apply_shouldCreateConfigForProfile_whenConfigIsNull() {
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("tb-core", "image:core");

        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template, "monolith");

        AgentAppProfile profile = new AgentAppProfile();
        // config is null

        rule.apply(profile, ctx);

        assertNotNull(profile.getConfig());
        assertInstanceOf(DockerComposeConfig.class, profile.getConfig());
        JsonNode result = ((DockerComposeConfig) profile.getConfig()).getCompose();
        assertEquals("image:core", result.get("tb-core").asText());
    }

    @Test
    void apply_shouldOverwriteExistingProfileConfig_withTemplateValues() {
        ObjectNode appCompose = MAPPER.createObjectNode().put("custom", "value");
        ObjectNode templateCompose = MAPPER.createObjectNode()
                .put("custom", "template-value")
                .put("added", "new");

        ComposeTypeChoiceStep choiceStep = createChoiceStep(Map.of("monolith", templateCompose));
        AgentAppTemplate template = createTemplate(List.of(choiceStep));
        AppConfigMergeCtx ctx = createCtx(template, "monolith");

        AgentAppProfile profile = new AgentAppProfile();
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(appCompose);
        profile.setConfig(config);

        rule.apply(profile, ctx);

        JsonNode result = ((DockerComposeConfig) profile.getConfig()).getCompose();
        assertEquals("template-value", result.get("custom").asText());
        assertEquals("new", result.get("added").asText());
    }

    // ==================== Helper methods ====================

    private AppConfigMergeCtx createCtx(String selectedComposeType) {
        return AppConfigMergeCtx.builder()
                .selectedComposeType(selectedComposeType)
                .build();
    }

    private AppConfigMergeCtx createCtx(AgentAppTemplate template, String selectedComposeType) {
        return AppConfigMergeCtx.builder()
                .template(template)
                .selectedComposeType(selectedComposeType)
                .build();
    }

    private ComposeStep createComposeStep() {
        ComposeStep step = new ComposeStep();
        step.setId(UUID.randomUUID());
        return step;
    }

    private ComposeTypeChoiceStep createChoiceStep(Map<String, ? extends JsonNode> templates) {
        ComposeTypeChoiceStep step = new ComposeTypeChoiceStep(UUID.randomUUID(), null, "Choose type");
        step.setComposeTemplates(Map.copyOf(templates));
        return step;
    }

    private ComposeStartStep createNonComposeStep() {
        ComposeStartStep step = new ComposeStartStep();
        step.setId(UUID.randomUUID());
        step.setTitle("Non-compose step");
        return step;
    }

    private AgentApplication createAppWithCompose(List<AgentAppStep> installSteps, JsonNode compose) {
        AgentApplication app = new AgentApplication();
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        app.setConfig(config);
        return app;
    }

    private AgentAppTemplate createTemplate(List<AgentAppStep> installSteps) {
        AgentAppTemplate template = new AgentAppTemplate();
        template.setStartSteps(new ArrayList<>(installSteps));
        return template;
    }

    private JsonNode getAppCompose(AgentApplication app) {
        return ((DockerComposeConfig) app.getConfig()).getCompose();
    }
}
