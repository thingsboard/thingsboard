/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenant;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainOutputLabelsUsage;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.script.RuleNodeTbelScriptEngine;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_NODE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class RuleChainController extends BaseController {

    public static final String RULE_CHAIN_ID = "ruleChainId";
    public static final String RULE_NODE_ID = "ruleNodeId";

    private static final int DEFAULT_PAGE_SIZE = 1000;

    public static final int TIMEOUT = 20;

    private static final String RULE_CHAIN_DESCRIPTION = "The rule chain object is lightweight and contains general information about the rule chain. " +
            "List of rule nodes and their connection is stored in a separate 'metadata' object.";
    private static final String RULE_CHAIN_METADATA_DESCRIPTION = "The metadata object contains information about the rule nodes and their connections.";
    private static final String TEST_SCRIPT_FUNCTION = "Execute the Script function and return the result. The format of request: \n\n"
            + MARKDOWN_CODE_BLOCK_START
            + "{\n" +
            "  \"script\": \"Your Function as String\",\n" +
            "  \"scriptType\": \"One of: update, generate, filter, switch, json, string\",\n" +
            "  \"argNames\": [\"msg\", \"metadata\", \"type\"],\n" +
            "  \"msg\": \"{\\\"temperature\\\": 42}\", \n" +
            "  \"metadata\": {\n" +
            "    \"deviceName\": \"Device A\",\n" +
            "    \"deviceType\": \"Thermometer\"\n" +
            "  },\n" +
            "  \"msgType\": \"POST_TELEMETRY_REQUEST\"\n" +
            "}"
            + MARKDOWN_CODE_BLOCK_END
            + "\n\n Expected result JSON contains \"output\" and \"error\".";

    @Autowired
    protected TbRuleChainService tbRuleChainService;

    @Autowired
    private EventService eventService;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    private TbelInvokeService tbelInvokeService;

    @Autowired(required = false)
    private ActorSystemContext actorContext;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled}")
    private boolean debugPerTenantEnabled;

    @Value("${tbel.enabled:true}")
    private boolean tbelEnabled;

    @ApiOperation(value = "Get Rule Chain (getRuleChainById)",
            notes = "Fetch the Rule Chain object based on the provided Rule Chain Id. " + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/{ruleChainId}")
    public RuleChain getRuleChainById(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        return checkRuleChain(ruleChainId, Operation.READ);
    }

    @ApiOperation(value = "Get Rule Chain output labels (getRuleChainOutputLabels)",
            notes = "Fetch the unique labels for the \"output\" Rule Nodes that belong to the Rule Chain based on the provided Rule Chain Id. "
                    + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/{ruleChainId}/output/labels")
    public Set<String> getRuleChainOutputLabels(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);
        return tbRuleChainService.getRuleChainOutputLabels(getTenantId(), ruleChainId);
    }

    @ApiOperation(value = "Get output labels usage (getRuleChainOutputLabelsUsage)",
            notes = "Fetch the list of rule chains and the relation types (labels) they use to process output of the current rule chain based on the provided Rule Chain Id. "
                    + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/{ruleChainId}/output/labels/usage")
    public List<RuleChainOutputLabelsUsage> getRuleChainOutputLabelsUsage(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);
        return tbRuleChainService.getOutputLabelUsage(getCurrentUser().getTenantId(), ruleChainId);
    }

    @ApiOperation(value = "Get Rule Chain (getRuleChainById)",
            notes = "Fetch the Rule Chain Metadata object based on the provided Rule Chain Id. " + RULE_CHAIN_METADATA_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/{ruleChainId}/metadata")
    public RuleChainMetaData getRuleChainMetaData(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.READ);
        return ruleChainService.loadRuleChainMetaData(getTenantId(), ruleChainId);
    }

    @ApiOperation(value = "Create Or Update Rule Chain (saveRuleChain)",
            notes = "Create or update the Rule Chain. When creating Rule Chain, platform generates Rule Chain Id as " + UUID_WIKI_LINK +
                    "The newly created Rule Chain Id will be present in the response. " +
                    "Specify existing Rule Chain id to update the rule chain. " +
                    "Referencing non-existing rule chain Id will cause 'Not Found' error." +
                    "\n\n" + RULE_CHAIN_DESCRIPTION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Rule Chain entity." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain")
    public RuleChain saveRuleChain(
            @Parameter(description = "A JSON value representing the rule chain.")
            @RequestBody RuleChain ruleChain) throws Exception {
        ruleChain.setTenantId(getCurrentUser().getTenantId());
        checkEntity(ruleChain.getId(), ruleChain, Resource.RULE_CHAIN);
        return tbRuleChainService.save(ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Create Default Rule Chain",
            notes = "Create rule chain from template, based on the specified name in the request. " +
                    "Creates the rule chain based on the template that is used to create root rule chain. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/device/default")
    public RuleChain saveRuleChain(
            @Parameter(description = "A JSON value representing the request.")
            @RequestBody DefaultRuleChainCreateRequest request) throws Exception {
        checkNotNull(request);
        checkParameter(request.getName(), "name");
        return tbRuleChainService.saveDefaultByName(getTenantId(), request, getCurrentUser());
    }

    @ApiOperation(value = "Set Root Rule Chain (setRootRuleChain)",
            notes = "Makes the rule chain to be root rule chain. Updates previous root rule chain as well. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/{ruleChainId}/root")
    public RuleChain setRootRuleChain(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.setRootRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Update Rule Chain Metadata",
            notes = "Updates the rule chain metadata. " + RULE_CHAIN_METADATA_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/metadata")
    public RuleChainMetaData saveRuleChainMetaData(
            @Parameter(description = "A JSON value representing the rule chain metadata.")
            @RequestBody RuleChainMetaData ruleChainMetaData,
            @Parameter(description = "Update related rule nodes.")
            @RequestParam(value = "updateRelated", required = false, defaultValue = "true") boolean updateRelated
    ) throws Exception {
        TenantId tenantId = getTenantId();
        if (debugPerTenantEnabled) {
            ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = actorContext.getDebugPerTenantLimits();
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.getOrDefault(tenantId, null);
            if (debugTbRateLimits != null) {
                debugPerTenantLimits.remove(tenantId, debugTbRateLimits);
            }
        }
        RuleChain ruleChain = checkRuleChain(ruleChainMetaData.getRuleChainId(), Operation.WRITE);

        return tbRuleChainService.saveRuleChainMetaData(tenantId, ruleChain, ruleChainMetaData, updateRelated, getCurrentUser());
    }

    @ApiOperation(value = "Get Rule Chains (getRuleChains)",
            notes = "Returns a page of Rule Chains owned by tenant. " + RULE_CHAIN_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/ruleChains", params = {"pageSize", "page"})
    public PageData<RuleChain> getRuleChains(
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = RULE_CHAIN_TYPE_DESCRIPTION, schema = @Schema(allowableValues = {"CORE", "EDGE"}))
            @RequestParam(value = "type", required = false) String typeStr,
            @Parameter(description = RULE_CHAIN_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "root"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        RuleChainType type = RuleChainType.CORE;
        if (StringUtils.isNotBlank(typeStr)) {
            type = RuleChainType.valueOf(typeStr);
        }
        return checkNotNull(ruleChainService.findTenantRuleChainsByType(tenantId, type, pageLink));
    }

    @ApiOperation(value = "Delete rule chain (deleteRuleChain)",
            notes = "Deletes the rule chain. Referencing non-existing rule chain Id will cause an error. " +
                    "Referencing rule chain that is used in the device profiles will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/ruleChain/{ruleChainId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRuleChain(
            @Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.DELETE);
        tbRuleChainService.delete(ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Get latest input message (getLatestRuleNodeDebugInput)",
            notes = "Gets the input message from the debug events for specified Rule Chain Id. " +
                    "Referencing non-existing rule chain Id will cause an error. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleNode/{ruleNodeId}/debugIn")
    public JsonNode getLatestRuleNodeDebugInput(
            @Parameter(description = RULE_NODE_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_NODE_ID) String strRuleNodeId) throws ThingsboardException {
        checkParameter(RULE_NODE_ID, strRuleNodeId);
        RuleNodeId ruleNodeId = new RuleNodeId(toUUID(strRuleNodeId));
        checkRuleNode(ruleNodeId, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        return Optional.ofNullable(eventService.findLatestDebugRuleNodeInEvent(tenantId, ruleNodeId))
                .map(EventInfo::getBody).orElse(null);
    }

    @ApiOperation(value = "Is TBEL script executor enabled",
            notes = "Returns 'True' if the TBEL script execution is enabled" + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/tbelEnabled")
    public Boolean isTbelEnabled() {
        return tbelEnabled;
    }

    @ApiOperation(value = "Test Script function",
            notes = TEST_SCRIPT_FUNCTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/testScript")
    public JsonNode testScript(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Test JS request. See API call description above.")
            @RequestBody JsonNode inputParams) {
        String script = inputParams.get("script").asText();
        String scriptType = inputParams.get("scriptType").asText();
        JsonNode argNamesJson = inputParams.get("argNames");
        String[] argNames = JacksonUtil.treeToValue(argNamesJson, String[].class);

        String data = inputParams.get("msg").asText();
        JsonNode metadataJson = inputParams.get("metadata");
        Map<String, String> metadata = JacksonUtil.convertValue(metadataJson, new TypeReference<>() {});
        String msgType = inputParams.get("msgType").asText();
        String output = "";
        String errorText = "";
        ScriptEngine engine = null;
        try {
            if (scriptLang == null) {
                scriptLang = ScriptLanguage.JS;
            }
            if (ScriptLanguage.JS.equals(scriptLang)) {
                engine = new RuleNodeJsScriptEngine(getTenantId(), jsInvokeService, script, argNames);
            } else {
                if (tbelInvokeService == null) {
                    throw new IllegalArgumentException("TBEL script engine is disabled!");
                }
                engine = new RuleNodeTbelScriptEngine(getTenantId(), tbelInvokeService, script, argNames);
            }

            var inMsg = TbMsg.newMsg()
                    .type(msgType)
                    .copyMetaData(new TbMsgMetaData(metadata))
                    .dataType(TbMsgDataType.JSON)
                    .data(data)
                    .build();

            output = switch (scriptType) {
                case "update" -> msgToOutput(engine.executeUpdateAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                case "generate" -> msgToOutput(engine.executeGenerateAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                case "filter" -> Boolean.toString(engine.executeFilterAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                case "switch" -> JacksonUtil.toString(engine.executeSwitchAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                case "json" -> JacksonUtil.toString(engine.executeJsonAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                case "string" -> engine.executeToStringAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS);
                default -> throw new IllegalArgumentException("Unsupported script type: " + scriptType);
            };
        } catch (Exception e) {
            log.error("Error evaluating JS function", e);
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            errorText = ObjectUtils.firstNonNull(rootCause.getMessage(), e.getMessage(), e.getClass().getSimpleName());
        } finally {
            if (engine != null) {
                engine.destroy();
            }
        }
        return JacksonUtil.newObjectNode()
                .put("output", output)
                .put("error", errorText);
    }

    @ApiOperation(value = "Export Rule Chains", notes = "Exports all tenant rule chains as one JSON." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/ruleChains/export", params = {"limit"})
    public RuleChainData exportRuleChains(
            @Parameter(description = "A limit of rule chains to export.", required = true)
            @RequestParam("limit") int limit) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = new PageLink(limit);
        return checkNotNull(ruleChainService.exportTenantRuleChains(tenantId, pageLink));
    }

    @ApiOperation(value = "Import Rule Chains", notes = "Imports all tenant rule chains as one JSON." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChains/import")
    public List<RuleChainImportResult> importRuleChains(
            @Parameter(description = "A JSON value representing the rule chains.")
            @RequestBody RuleChainData ruleChainData,
            @Parameter(description = "Enables overwrite for existing rule chains with the same name.")
            @RequestParam(required = false, defaultValue = "false") boolean overwrite) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return ruleChainService.importTenantRuleChains(tenantId, ruleChainData, overwrite, tbRuleChainService::updateRuleNodeConfiguration);
    }

    private String msgToOutput(TbMsg msg) {
        JsonNode resultNode = convertMsgToOut(msg);
        return JacksonUtil.toString(resultNode);
    }

    private String msgToOutput(List<TbMsg> msgs) {
        JsonNode resultNode;
        if (msgs.size() > 1) {
            resultNode = JacksonUtil.newArrayNode();
            for (TbMsg msg : msgs) {
                JsonNode convertedData = convertMsgToOut(msg);
                ((ArrayNode) resultNode).add(convertedData);
            }
        } else {
            resultNode = convertMsgToOut(msgs.get(0));
        }
        return JacksonUtil.toString(resultNode);
    }

    private JsonNode convertMsgToOut(TbMsg msg) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        if (!StringUtils.isEmpty(msg.getData())) {
            msgData.set("msg", JacksonUtil.toJsonNode(msg.getData()));
        }
        Map<String, String> metadata = msg.getMetaData().getData();
        msgData.set("metadata", JacksonUtil.valueToTree(metadata));
        msgData.put("msgType", msg.getType());
        return msgData;
    }

    @ApiOperation(value = "Assign rule chain to edge (assignRuleChainToEdge)",
            notes = "Creates assignment of an existing rule chain to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment rule chain " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once rule chain will be delivered to edge service, it's going to start processing messages locally. " +
                    "\n\nOnly rule chain with type 'EDGE' can be assigned to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/edge/{edgeId}/ruleChain/{ruleChainId}")
    public RuleChain assignRuleChainToEdge(@PathVariable("edgeId") String strEdgeId,
                                           @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);

        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.READ);

        return tbRuleChainService.assignRuleChainToEdge(getTenantId(), ruleChain, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign rule chain from edge (unassignRuleChainFromEdge)",
            notes = "Clears assignment of the rule chain to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove rule chain " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove rule chain locally." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/edge/{edgeId}/ruleChain/{ruleChainId}")
    public RuleChain unassignRuleChainFromEdge(@PathVariable("edgeId") String strEdgeId,
                                               @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.READ);

        return tbRuleChainService.unassignRuleChainFromEdge(getTenantId(), ruleChain, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Rule Chains (getEdgeRuleChains)",
            notes = "Returns a page of Rule Chains assigned to the specified edge. " + RULE_CHAIN_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/edge/{edgeId}/ruleChains", params = {"pageSize", "page"})
    public PageData<RuleChain> getEdgeRuleChains(
            @Parameter(description = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @Parameter(description = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @Parameter(description = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Parameter(description = RULE_CHAIN_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "root"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
    }

    @ApiOperation(value = "Set Edge Template Root Rule Chain (setEdgeTemplateRootRuleChain)",
            notes = "Makes the rule chain to be root rule chain for any new edge that will be created. " +
                    "Does not update root rule chain for already created edges. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/{ruleChainId}/edgeTemplateRoot")
    public RuleChain setEdgeTemplateRootRuleChain(@Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
                                                  @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.setEdgeTemplateRootRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Set Auto Assign To Edge Rule Chain (setAutoAssignToEdgeRuleChain)",
            notes = "Makes the rule chain to be automatically assigned for any new edge that will be created. " +
                    "Does not assign this rule chain for already created edges. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/ruleChain/{ruleChainId}/autoAssignToEdge")
    public RuleChain setAutoAssignToEdgeRuleChain(@Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
                                                  @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.setAutoAssignToEdgeRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Unset Auto Assign To Edge Rule Chain (unsetAutoAssignToEdgeRuleChain)",
            notes = "Removes the rule chain from the list of rule chains that are going to be automatically assigned for any new edge that will be created. " +
                    "Does not unassign this rule chain for already assigned edges. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping("/ruleChain/{ruleChainId}/autoAssignToEdge")
    public RuleChain unsetAutoAssignToEdgeRuleChain(@Parameter(description = RULE_CHAIN_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.unsetAutoAssignToEdgeRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    // TODO: @voba refactor this - add new config to edge rule chain to set it as auto-assign
    @ApiOperation(value = "Get Auto Assign To Edge Rule Chains (getAutoAssignToEdgeRuleChains)",
            notes = "Returns a list of Rule Chains that will be assigned to a newly created edge. " + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/ruleChain/autoAssignToEdgeRuleChains")
    public List<RuleChain> getAutoAssignToEdgeRuleChains() throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        List<RuleChain> result = new ArrayList<>();
        PageDataIterableByTenant<RuleChain> autoAssignRuleChainsIterator =
                new PageDataIterableByTenant<>(ruleChainService::findAutoAssignToEdgeRuleChainsByTenantId, tenantId, DEFAULT_PAGE_SIZE);
        for (RuleChain ruleChain : autoAssignRuleChainsIterator) {
            result.add(ruleChain);
        }
        return checkNotNull(result);
    }

    @ApiOperation(value = "Get Rule Chains By Ids (getRuleChainsByIds)",
            notes = "Requested rule chains must be owned by tenant which is performing the request. " +
                    NEW_LINE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/ruleChains", params = {"ruleChainIds"})
    public List<RuleChain> getRuleChainsByIds(
            @Parameter(description = "A list of rule chain ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("ruleChainIds") String[] strRuleChainIds) throws Exception {
        checkArrayParameter("ruleChainIds", strRuleChainIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<RuleChainId> ruleChainIds = new ArrayList<>();
        for (String strRuleChainId : strRuleChainIds) {
            ruleChainIds.add(new RuleChainId(toUUID(strRuleChainId)));
        }
        return Objects.requireNonNull(checkNotNull(ruleChainService.findRuleChainsByIdsAsync(tenantId, ruleChainIds).get()))
                .stream()
                .filter(e -> {
                    try {
                        return accessControlService.hasPermission(user, Resource.RULE_CHAIN, Operation.READ, e.getId(), e);
                    } catch (ThingsboardException ex) {
                        return false;
                    }
                })
                .toList();
    }

}
