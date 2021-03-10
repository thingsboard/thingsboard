/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityConfig;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityConfigId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rule.DefaultRuleChainCreateRequest;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.install.InstallScripts;
import org.thingsboard.server.service.script.JsInvokeService;
import org.thingsboard.server.service.script.RuleNodeJsScriptEngine;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.DEFAULT_RESTORED_ENTITY_NAME_TPL;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class RuleChainController extends BaseController {

    public static final String RULE_CHAIN_ID = "ruleChainId";
    public static final String RULE_NODE_ID = "ruleNodeId";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private EventService eventService;

    @Autowired
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    private ActorSystemContext actorContext;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled}")
    private boolean debugPerTenantEnabled;

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}", method = RequestMethod.GET)
    @ResponseBody
    public RuleChain getRuleChainById(@PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            return checkRuleChain(ruleChainId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/metadata", method = RequestMethod.GET)
    @ResponseBody
    public RuleChainMetaData getRuleChainMetaData(@PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            checkRuleChain(ruleChainId, Operation.READ);
            return ruleChainService.loadRuleChainMetaData(getTenantId(), ruleChainId);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain saveRuleChain(@RequestBody RuleChain ruleChain) throws ThingsboardException {
        try {
            boolean created = ruleChain.getId() == null;
            ruleChain.setTenantId(getCurrentUser().getTenantId());

            checkEntity(ruleChain.getId(), ruleChain, Resource.RULE_CHAIN);

            RuleChain savedRuleChain = checkNotNull(ruleChainService.saveRuleChain(ruleChain));

            tbClusterService.onEntityStateChange(ruleChain.getTenantId(), savedRuleChain.getId(),
                    created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedRuleChain.getId(), savedRuleChain,
                    null,
                    created ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedRuleChain;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE_CHAIN), ruleChain,
                    null, ruleChain.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/device/default", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain saveRuleChain(@RequestBody DefaultRuleChainCreateRequest request) throws ThingsboardException {
        try {
            checkNotNull(request);
            checkParameter(request.getName(), "name");

            RuleChain savedRuleChain = installScripts.createDefaultRuleChain(getCurrentUser().getTenantId(), request.getName());

            tbClusterService.onEntityStateChange(savedRuleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.CREATED);

            logEntityAction(savedRuleChain.getId(), savedRuleChain, null, ActionType.ADDED, null);

            return savedRuleChain;
        } catch (Exception e) {
            RuleChain ruleChain = new RuleChain();
            ruleChain.setName(request.getName());
            logEntityAction(emptyId(EntityType.RULE_CHAIN), ruleChain, null, ActionType.ADDED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    public RuleChain setRootRuleChain(@PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
            TenantId tenantId = getCurrentUser().getTenantId();
            RuleChain previousRootRuleChain = ruleChainService.getRootTenantRuleChain(tenantId);
            if (ruleChainService.setRootRuleChain(getTenantId(), ruleChainId)) {
                if (previousRootRuleChain != null) {
                    previousRootRuleChain = ruleChainService.findRuleChainById(getTenantId(), previousRootRuleChain.getId());

                    tbClusterService.onEntityStateChange(previousRootRuleChain.getTenantId(), previousRootRuleChain.getId(),
                            ComponentLifecycleEvent.UPDATED);

                    logEntityAction(previousRootRuleChain.getId(), previousRootRuleChain,
                            null, ActionType.UPDATED, null);
                }
                ruleChain = ruleChainService.findRuleChainById(getTenantId(), ruleChainId);

                tbClusterService.onEntityStateChange(ruleChain.getTenantId(), ruleChain.getId(),
                        ComponentLifecycleEvent.UPDATED);

                logEntityAction(ruleChain.getId(), ruleChain,
                        null, ActionType.UPDATED, null);

            }
            return ruleChain;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.RULE_CHAIN),
                    null,
                    null,
                    ActionType.UPDATED, e, strRuleChainId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/metadata", method = RequestMethod.POST)
    @ResponseBody
    public RuleChainMetaData saveRuleChainMetaData(@RequestBody RuleChainMetaData ruleChainMetaData, @RequestParam(required = false) String comment) throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId();
            if (debugPerTenantEnabled) {
                ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = actorContext.getDebugPerTenantLimits();
                DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.getOrDefault(tenantId, null);
                if (debugTbRateLimits != null) {
                    debugPerTenantLimits.remove(tenantId, debugTbRateLimits);
                }
            }
            ObjectNode additionalInfo = getEntityConfigAdditionalInfo(comment);
            RuleChain ruleChain = checkRuleChain(ruleChainMetaData.getRuleChainId(), Operation.WRITE);
            RuleChainMetaData savedRuleChainMetaData = checkNotNull(ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData));
            entityConfigService.saveEntityConfigForEntity(getTenantId(), ruleChain.getId(), objectMapper.valueToTree(savedRuleChainMetaData), additionalInfo);
            tbClusterService.onEntityStateChange(ruleChain.getTenantId(), ruleChain.getId(), ComponentLifecycleEvent.UPDATED);

            logEntityAction(ruleChain.getId(), ruleChain,
                    null,
                    ActionType.UPDATED, null, ruleChainMetaData);

            return savedRuleChainMetaData;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.RULE_CHAIN), null,
                    null, ActionType.UPDATED, e, ruleChainMetaData);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/entityConfig/{entityConfigId}/restore", method = RequestMethod.POST)
    @ResponseBody
    public RuleChainMetaData restoreRuleChainMetaData(@PathVariable(RULE_CHAIN_ID) String strRuleChainId,
                                                      @PathVariable("entityConfigId") String strEntityConfigId,
                                                      @RequestParam(required = false) String comment) throws ThingsboardException {
        try {
            TenantId tenantId = getTenantId();
            if (debugPerTenantEnabled) {
                ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = actorContext.getDebugPerTenantLimits();
                DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.getOrDefault(tenantId, null);
                if (debugTbRateLimits != null) {
                    debugPerTenantLimits.remove(tenantId, debugTbRateLimits);
                }
            }
            checkParameter(RULE_CHAIN_ID, strRuleChainId);
            checkParameter("entityConfigId", strEntityConfigId);

            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
            EntityConfigId entityConfigId = new EntityConfigId(toUUID(strEntityConfigId));
            ObjectNode additionalInfo = getEntityConfigAdditionalInfo(comment);
            RuleChainMetaData ruleChainMetaData = ruleChainService.restoreRuleChainMetaData(getTenantId(), ruleChainId, entityConfigId, additionalInfo);
            RuleChainMetaData savedRuleChainMetaData = checkNotNull(ruleChainService.saveRuleChainMetaData(tenantId, ruleChainMetaData));
            if (savedRuleChainMetaData != null) {
                tbClusterService.onEntityStateChange(ruleChain.getTenantId(), ruleChain.getId(), ComponentLifecycleEvent.UPDATED);

                logEntityAction(ruleChain.getId(), ruleChain,
                        null,
                        ActionType.UPDATED, null, savedRuleChainMetaData);
            }
            return savedRuleChainMetaData;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.RULE_CHAIN), null,
                    null, ActionType.UPDATED, e, null);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}/entityConfig/{entityConfigId}/restoreAsNew", method = RequestMethod.POST)
    @ResponseBody
    public RuleChainMetaData restoreRuleChainMetaData(@PathVariable(RULE_CHAIN_ID) String strRuleChainId,
                                                      @PathVariable("entityConfigId") String strEntityConfigId,
                                                      @RequestParam(required = false) String comment,
                                                      @RequestParam(required = false) String name) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        checkParameter("entityConfigId", strEntityConfigId);
        EntityConfigId entityConfigId = new EntityConfigId(toUUID(strEntityConfigId));
        EntityConfig entityConfig = entityConfigService.getEntityConfigById(tenantId, entityConfigId);
        if (entityConfig == null) {
            throw new EntityNotFoundException(String.format("EntityConfig with id [%s] not found", entityConfigId.getId()));
        }

        RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.WRITE);
        if (name == null) {
            name = String.format(DEFAULT_RESTORED_ENTITY_NAME_TPL, ruleChain.getName(), entityConfig.getVersion());
        }
        RuleChain newRuleChain = new RuleChain();
        newRuleChain.setTenantId(tenantId);
        newRuleChain.setName(name);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(newRuleChain);

        tbClusterService.onEntityStateChange(savedRuleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.CREATED);
        logEntityAction(savedRuleChain.getId(), savedRuleChain, null, ActionType.ADDED, null);

        RuleChainMetaData restoredRuleChainMetadata = ruleChainService.getRuleChainMetaDataByEntityConfigId(tenantId, ruleChainId, entityConfigId);
        restoredRuleChainMetadata.setRuleChainId(savedRuleChain.getId());
        RuleChainMetaData savedRuleChainMetaData = checkNotNull(ruleChainService.saveRuleChainMetaData(tenantId, restoredRuleChainMetadata));

        ObjectNode additionalInfo = getEntityConfigAdditionalInfo(comment);
        entityConfigService.saveEntityConfigForEntity(getTenantId(), savedRuleChain.getId(), objectMapper.valueToTree(savedRuleChainMetaData), additionalInfo);
        if (savedRuleChainMetaData != null) {
            tbClusterService.onEntityStateChange(savedRuleChain.getTenantId(), savedRuleChain.getId(), ComponentLifecycleEvent.UPDATED);

            logEntityAction(savedRuleChain.getId(), savedRuleChain,
                    null,
                    ActionType.UPDATED, null, savedRuleChainMetaData);
        }
        return savedRuleChainMetaData;
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChains", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<RuleChain> getRuleChains(
            @RequestParam int pageSize,
            @RequestParam int page,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(ruleChainService.findTenantRuleChains(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/{ruleChainId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteRuleChain(@PathVariable(RULE_CHAIN_ID) String strRuleChainId) throws ThingsboardException {
        checkParameter(RULE_CHAIN_ID, strRuleChainId);
        try {
            RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
            RuleChain ruleChain = checkRuleChain(ruleChainId, Operation.DELETE);

            List<RuleNode> referencingRuleNodes = ruleChainService.getReferencingRuleChainNodes(getTenantId(), ruleChainId);

            Set<RuleChainId> referencingRuleChainIds = referencingRuleNodes.stream().map(RuleNode::getRuleChainId).collect(Collectors.toSet());

            ruleChainService.deleteRuleChainById(getTenantId(), ruleChainId);

            referencingRuleChainIds.remove(ruleChain.getId());

            referencingRuleChainIds.forEach(referencingRuleChainId ->
                    tbClusterService.onEntityStateChange(ruleChain.getTenantId(), referencingRuleChainId, ComponentLifecycleEvent.UPDATED));

            tbClusterService.onEntityStateChange(ruleChain.getTenantId(), ruleChain.getId(), ComponentLifecycleEvent.DELETED);

            logEntityAction(ruleChainId, ruleChain,
                    null,
                    ActionType.DELETED, null, strRuleChainId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.RULE_CHAIN),
                    null,
                    null,
                    ActionType.DELETED, e, strRuleChainId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleNode/{ruleNodeId}/debugIn", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getLatestRuleNodeDebugInput(@PathVariable(RULE_NODE_ID) String strRuleNodeId) throws ThingsboardException {
        checkParameter(RULE_NODE_ID, strRuleNodeId);
        try {
            RuleNodeId ruleNodeId = new RuleNodeId(toUUID(strRuleNodeId));
            checkRuleNode(ruleNodeId, Operation.READ);
            TenantId tenantId = getCurrentUser().getTenantId();
            List<Event> events = eventService.findLatestEvents(tenantId, ruleNodeId, DataConstants.DEBUG_RULE_NODE, 2);
            JsonNode result = null;
            if (events != null) {
                for (Event event : events) {
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChain/testScript", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode testScript(@RequestBody JsonNode inputParams) throws ThingsboardException {
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
                engine = new RuleNodeJsScriptEngine(getTenantId(), jsInvokeService, getCurrentUser().getId(), script, argNames);
                TbMsg inMsg = TbMsg.newMsg(msgType, null, new TbMsgMetaData(metadata), TbMsgDataType.JSON, data);
                switch (scriptType) {
                    case "update":
                        output = msgToOutput(engine.executeUpdate(inMsg));
                        break;
                    case "generate":
                        output = msgToOutput(engine.executeGenerate(inMsg));
                        break;
                    case "filter":
                        boolean result = engine.executeFilter(inMsg);
                        output = Boolean.toString(result);
                        break;
                    case "switch":
                        Set<String> states = engine.executeSwitch(inMsg);
                        output = objectMapper.writeValueAsString(states);
                        break;
                    case "json":
                        JsonNode json = engine.executeJson(inMsg);
                        output = objectMapper.writeValueAsString(json);
                        break;
                    case "string":
                        output = engine.executeToString(inMsg);
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

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChains/export", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public RuleChainData exportRuleChains(@RequestParam("limit") int limit) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = new PageLink(limit);
            return checkNotNull(ruleChainService.exportTenantRuleChains(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/ruleChains/import", method = RequestMethod.POST)
    @ResponseBody
    public void importRuleChains(@RequestBody RuleChainData ruleChainData, @RequestParam(required = false, defaultValue = "false") boolean overwrite) throws ThingsboardException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            List<RuleChainImportResult> importResults = ruleChainService.importTenantRuleChains(tenantId, ruleChainData, overwrite);
            if (!CollectionUtils.isEmpty(importResults)) {
                for (RuleChainImportResult importResult : importResults) {
                    tbClusterService.onEntityStateChange(importResult.getTenantId(), importResult.getRuleChainId(), importResult.getLifecycleEvent());
                }
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private String msgToOutput(TbMsg msg) throws Exception {
        ObjectNode msgData = objectMapper.createObjectNode();
        if (!StringUtils.isEmpty(msg.getData())) {
            msgData.set("msg", objectMapper.readTree(msg.getData()));
        }
        Map<String, String> metadata = msg.getMetaData().getData();
        msgData.set("metadata", objectMapper.valueToTree(metadata));
        msgData.put("msgType", msg.getType());
        return objectMapper.writeValueAsString(msgData);
    }

}
