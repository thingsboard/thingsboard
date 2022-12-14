/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenant;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainOutputLabelsUsage;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.rule.TbRuleChainService;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.script.RuleNodeTbelScriptEngine;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_TYPES_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.RULE_CHAIN_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RULE_NODE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();
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
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}", method = RequestMethod.GET)
    @ResponseBody
    public RuleChain getRuleChainById(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            return checkRuleChain(ruleChainId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Rule Chain output labels (getRuleChainOutputLabels)",
            notes = "Fetch the unique labels for the \"output\" Rule Nodes that belong to the Rule Chain based on the provided Rule Chain Id. "
                    + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/output/labels", method = RequestMethod.GET)
    @ResponseBody
    public Set<String> getRuleChainOutputLabels(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.READ);
            return tbRuleChainService.getRuleChainOutputLabels(getTenantId(), ruleChainId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get output labels usage (getRuleChainOutputLabelsUsage)",
            notes = "Fetch the list of rule chains and the relation types (labels) they use to process output of the current rule chain based on the provided Rule Chain Id. "
                    + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/output/labels/usage", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleChainOutputLabelsUsage> getRuleChainOutputLabelsUsage(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.READ);
            return tbRuleChainService.getOutputLabelUsage(getCurrentUser().getTenantId(), ruleChainId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Rule Chain (getRuleChainById)",
            notes = "Fetch the Rule Chain Metadata object based on the provided Rule Chain Id. " + RULE_CHAIN_METADATA_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/metadata", method = RequestMethod.GET)
    @ResponseBody
    public RuleChainMetaData getRuleChainMetaData(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.READ);
            return ruleChainService.loadRuleChainMetaData(getTenantId(), ruleChainId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Rule Chain (saveRuleChain)",
            notes = "Create or update the Rule Chain. When creating Rule Chain, platform generates Rule Chain Id as " + UUID_WIKI_LINK +
                    "The newly created Rule Chain Id will be present in the response. " +
                    "Specify existing Rule Chain id to update the rule chain. " +
                    "Referencing non-existing rule chain Id will cause 'Not Found' error." +
                    "\n\n" + RULE_CHAIN_DESCRIPTION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Rule Chain entity." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain saveRuleChain(
            @ApiParam(value = "A JSON value representing the rule chain.")
            @RequestBody RuleChain ruleChain) throws Exception {
        ruleChain.setTenantId(getCurrentUser().getTenantId());
        checkEntity(ruleChain.getId(), ruleChain, Resource.RULE_CHAIN);
        return tbRuleChainService.save(ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Create Default Rule Chain",
            notes = "Create rule chain from template, based on the specified name in the request. " +
                    "Creates the rule chain based on the template that is used to create root rule chain. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/device/default", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain saveRuleChain(
            @ApiParam(value = "A JSON value representing the request.")
            @RequestBody DefaultRuleChainCreateRequest request) throws Exception {
        checkNotNull(request);
        checkParameter(request.getName(), "name");
        return tbRuleChainService.saveDefaultByName(getTenantId(), request, getCurrentUser());
    }

    @ApiOperation(value = "Set Root Rule Chain (setRootRuleChain)",
            notes = "Makes the rule chain to be root rule chain. Updates previous root rule chain as well. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain setRootRuleChain(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.setRootRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Update Rule Chain Metadata",
            notes = "Updates the rule chain metadata. " + RULE_CHAIN_METADATA_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/metadata", method = RequestMethod.POST)
    @ResponseBody
    public RuleChainMetaData saveRuleChainMetaData(
            @ApiParam(value = "A JSON value representing the rule chain metadata.")
            @RequestBody RuleChainMetaData ruleChainMetaData,
            @ApiParam(value = "Update related rule nodes.")
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
    @RequestMapping(value = "/ruleChains", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<RuleChain> getRuleChains(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = RULE_CHAIN_TYPE_DESCRIPTION, allowableValues = RULE_CHAIN_TYPES_ALLOWABLE_VALUES)
            @RequestParam(value = "type", required = false) String typeStr,
            @ApiParam(value = RULE_CHAIN_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RULE_CHAIN_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            RuleChainType type = RuleChainType.CORE;
            if (typeStr != null && typeStr.trim().length() > 0) {
                type = RuleChainType.valueOf(typeStr);
            }
            return checkNotNull(ruleChainService.findTenantRuleChainsByType(tenantId, type, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete rule chain (deleteRuleChain)",
            notes = "Deletes the rule chain. Referencing non-existing rule chain Id will cause an error. " +
                    "Referencing rule chain that is used in the device profiles will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRuleChain(
            @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.DELETE);
        tbRuleChainService.delete(ruleChain, getCurrentUser());
    }

    @ApiOperation(value = "Get latest input message (getLatestRuleNodeDebugInput)",
            notes = "Gets the input message from the debug events for specified Rule Chain Id. " +
                    "Referencing non-existing rule chain Id will cause an error. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleNode/{ruleNodeId}/debugIn", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getLatestRuleNodeDebugInput(
            @ApiParam(value = RULE_NODE_ID_PARAM_DESCRIPTION)
            @PathVariable(RULE_NODE_ID) String strRuleNodeId) throws ThingsboardException {
        checkParameter(RULE_NODE_ID, strRuleNodeId);
        try {
            RuleNodeId ruleNodeId = new RuleNodeId(toUUID(strRuleNodeId));
            checkRuleNode(ruleNodeId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            List<EventInfo> events = eventService.findLatestEvents(tenantId, ruleNodeId, EventType.DEBUG_RULE_NODE, 2);
            JsonNode result = null;
            if (events != null) {
                for (EventInfo event : events) {
                    JsonNode body = event.getBody();
                    if (body.has("type") && body.get("type").asText().equals("IN")) {
                        result = body;
                        break;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Is TBEL script executor enabled",
            notes = "Returns 'True' if the TBEL script execution is enabled" + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/tbelEnabled", method = RequestMethod.GET)
    @ResponseBody
    public Boolean isTbelEnabled() {
        return tbelEnabled;
    }

    @ApiOperation(value = "Test Script function",
            notes = TEST_SCRIPT_FUNCTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/testScript", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testScript(
            @ApiParam(value = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @ApiParam(value = "Test JS request. See API call description above.")
            @RequestBody JsonNode inputParams) throws ThingsboardException {
        try {
            String script = inputParams.get("script").asText();
            String scriptType = inputParams.get("scriptType").asText();
            JsonNode argNamesJson = inputParams.get("argNames");
            String[] argNames = objectMapper.treeToValue(argNamesJson, String[].class);

            String data = inputParams.get("msg").asText();
            JsonNode metadataJson = inputParams.get("metadata");
            Map<String, String> metadata = objectMapper.convertValue(metadataJson, new TypeReference<Map<String, String>>() {
            });
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
                TbMsg inMsg = TbMsg.newMsg(msgType, null, new TbMsgMetaData(metadata), TbMsgDataType.JSON, data);
                switch (scriptType) {
                    case "update":
                        output = msgToOutput(engine.executeUpdateAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                        break;
                    case "generate":
                        output = msgToOutput(engine.executeGenerateAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS));
                        break;
                    case "filter":
                        boolean result = engine.executeFilterAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS);
                        output = Boolean.toString(result);
                        break;
                    case "switch":
                        Set<String> states = engine.executeSwitchAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS);
                        output = objectMapper.writeValueAsString(states);
                        break;
                    case "json":
                        JsonNode json = engine.executeJsonAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS);
                        output = objectMapper.writeValueAsString(json);
                        break;
                    case "string":
                        output = engine.executeToStringAsync(inMsg).get(TIMEOUT, TimeUnit.SECONDS);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported script type: " + scriptType);
                }
            } catch (Exception e) {
                log.error("Error evaluating JS function", e);
                errorText = e.getMessage();
            } finally {
                if (engine != null) {
                    engine.destroy();
                }
            }
            ObjectNode result = objectMapper.createObjectNode();
            result.put("output", output);
            result.put("error", errorText);
            return result;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Export Rule Chains", notes = "Exports all tenant rule chains as one JSON." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChains/export", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public RuleChainData exportRuleChains(
            @ApiParam(value = "A limit of rule chains to export.", required = true)
            @RequestParam("limit") int limit) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = new PageLink(limit);
            return checkNotNull(ruleChainService.exportTenantRuleChains(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Import Rule Chains", notes = "Imports all tenant rule chains as one JSON." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChains/import", method = RequestMethod.POST)
    @ResponseBody
    public List<RuleChainImportResult> importRuleChains(
            @ApiParam(value = "A JSON value representing the rule chains.")
            @RequestBody RuleChainData ruleChainData,
            @ApiParam(value = "Enables overwrite for existing rule chains with the same name.")
            @RequestParam(required = false, defaultValue = "false") boolean overwrite) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            List<RuleChainImportResult> importResults = ruleChainService.importTenantRuleChains(tenantId, ruleChainData, overwrite);
            for (RuleChainImportResult importResult : importResults) {
                if (importResult.getError() == null) {
                    tbClusterService.broadcastEntityStateChangeEvent(importResult.getTenantId(), importResult.getRuleChainId(),
                            importResult.isUpdated() ? ComponentLifecycleEvent.UPDATED : ComponentLifecycleEvent.CREATED);
                }
            }
            return importResults;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String msgToOutput(TbMsg msg) throws Exception {
        JsonNode resultNode = convertMsgToOut(msg);
        return objectMapper.writeValueAsString(resultNode);
    }

    private String msgToOutput(List<TbMsg> msgs) throws Exception {
        JsonNode resultNode;
        if (msgs.size() > 1) {
            resultNode = objectMapper.createArrayNode();
            for (TbMsg msg : msgs) {
                JsonNode convertedData = convertMsgToOut(msg);
                ((ArrayNode) resultNode).add(convertedData);
            }
        } else {
            resultNode = convertMsgToOut(msgs.get(0));
        }
        return objectMapper.writeValueAsString(resultNode);
    }

    private JsonNode convertMsgToOut(TbMsg msg) throws Exception {
        ObjectNode msgData = objectMapper.createObjectNode();
        if (!StringUtils.isEmpty(msg.getData())) {
            msgData.set("msg", objectMapper.readTree(msg.getData()));
        }
        Map<String, String> metadata = msg.getMetaData().getData();
        msgData.set("metadata", objectMapper.valueToTree(metadata));
        msgData.put("msgType", msg.getType());
        return msgData;
    }

    @ApiOperation(value = "Assign rule chain to edge (assignRuleChainToEdge)",
            notes = "Creates assignment of an existing rule chain to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment rule chain " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once rule chain will be delivered to edge service, it's going to start processing messages locally. " +
                    "\n\nOnly rule chain with type 'EDGE' can be assigned to edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/ruleChain/{ruleChainId}", method = RequestMethod.POST)
    @ResponseBody
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
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove rule chain locally." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/ruleChain/{ruleChainId}", method = RequestMethod.DELETE)
    @ResponseBody
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/ruleNode/{ruleNodeId}/stats")
    public void clearRuleNodeStats(@ApiParam(value = RULE_NODE_ID_PARAM_DESCRIPTION)
                                   @PathVariable(RULE_NODE_ID) String strRuleNodeId) throws ThingsboardException {
        checkParameter(RULE_NODE_ID, strRuleNodeId);
        try {
            RuleNodeId ruleNodeId = new RuleNodeId(toUUID(strRuleNodeId));
            RuleNode ruleNode = checkRuleNode(ruleNodeId, Operation.WRITE);

            ruleChainService.clearRuleNodeStats(getTenantId(), ruleNode.getRuleChainId(), ruleNodeId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/ruleChain/{ruleChainId}/clearRuleNodesStats")
    @ResponseStatus(value = HttpStatus.OK)
    public void clearRuleChainErrors(@ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION, required = true)
                                              @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);

            ruleChainService.clearRuleChainErrors(getTenantId(), ruleChain.getId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edge Rule Chains (getEdgeRuleChains)",
            notes = "Returns a page of Rule Chains assigned to the specified edge. " + RULE_CHAIN_DESCRIPTION + PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/ruleChains", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<RuleChain> getEdgeRuleChains(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = RULE_CHAIN_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RULE_CHAIN_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Set Edge Template Root Rule Chain (setEdgeTemplateRootRuleChain)",
            notes = "Makes the rule chain to be root rule chain for any new edge that will be created. " +
                    "Does not update root rule chain for already created edges. " + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/edgeTemplateRoot", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain setEdgeTemplateRootRuleChain(@ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
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
    @RequestMapping(value = "/ruleChain/{ruleChainId}/autoAssignToEdge", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain setAutoAssignToEdgeRuleChain(@ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
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
    @RequestMapping(value = "/ruleChain/{ruleChainId}/autoAssignToEdge", method = RequestMethod.DELETE)
    @ResponseBody
    public RuleChain unsetAutoAssignToEdgeRuleChain(@ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION)
                                                    @PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        return tbRuleChainService.unsetAutoAssignToEdgeRuleChain(getTenantId(), ruleChain, getCurrentUser());
    }

    // TODO: @voba refactor this - add new config to edge rule chain to set it as auto-assign
    @ApiOperation(value = "Get Auto Assign To Edge Rule Chains (getAutoAssignToEdgeRuleChains)",
            notes = "Returns a list of Rule Chains that will be assigned to a newly created edge. " + RULE_CHAIN_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/autoAssignToEdgeRuleChains", method = RequestMethod.GET)
    @ResponseBody
    public List<RuleChain> getAutoAssignToEdgeRuleChains() throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            List<RuleChain> result = new ArrayList<>();
            PageDataIterableByTenant<RuleChain> autoAssignRuleChainsIterator =
                    new PageDataIterableByTenant<>(ruleChainService::findAutoAssignToEdgeRuleChainsByTenantId, tenantId, DEFAULT_PAGE_SIZE);
            for (RuleChain ruleChain : autoAssignRuleChainsIterator) {
                result.add(ruleChain);
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
