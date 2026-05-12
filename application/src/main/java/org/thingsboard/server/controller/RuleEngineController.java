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
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rule.engine.EntityAclEntry;
import org.thingsboard.server.common.data.rule.engine.RuleEngineV2Request;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RULE_ENGINE_URL_PREFIX)
@Slf4j
public class RuleEngineController extends BaseController {

    private static final String MSG_DESCRIPTION_PREFIX = "Creates the Message with type 'REST_API_REQUEST' and payload taken from the request body. ";
    private static final String MSG_DESCRIPTION = "This method allows you to extend the regular platform API with the power of Rule Engine. You may use default and custom rule nodes to handle the message. " +
            "The generated message contains two important metadata fields:\n\n" +
            " * **'serviceId'** to identify the platform server that received the request;\n" +
            " * **'requestUUID'** to identify the request and route possible response from the Rule Engine;\n\n" +
            "Use **'rest call reply'** rule node to push the reply from rule engine back as a REST API call response. ";

    private static final String V2_DESCRIPTION = "Variant of the Rule Engine REST API that enriches the forwarded `TbMsg` with two " +
            "server-authoritative metadata keys before pushing it to the rule engine:\n\n" +
            " * **`tb_aclSnapshot`** — a JSON array of `{entityId, allowed[]}` computed for every entity the caller lists in `aclEntities`. " +
            "The `allowed` array contains the `Operation` values the caller has on the specific instance;\n" +
            " * **`tb_userId`** — UUID of the calling user, intended for audit logging inside rule chains.\n\n" +
            "Caller-supplied values for either key are overwritten by the platform. This endpoint preserves the existing v1 behavior " +
            "(timeout, queue routing, REST Call Reply) — the enrichment is additive.\n\n" +
            "Note: `SYS_ADMIN` callers operate against the system tenant, so tenant-scoped entities (DEVICE, ASSET, ...) " +
            "passed in `aclEntities` will produce `allowed=[]`.";

    @Value("${server.rest.rule_engine.response_timeout:10000}")
    public int defaultResponseTimeout;

    /**
     * Maximum number of entities accepted for ACL enrichment in a single
     * {@code /api/rule-engine/v2} request. Each entity triggers N permission checks
     * (one per {@link Operation} value), so the bound prevents excessive work per call.
     * Set to 0 to disable the bound entirely.
     */
    @Value("${rule-engine.acl.max-entities:20}")
    private int maxAclEntities;

    @Autowired
    private RuleEngineCallService ruleEngineCallService;
    @Autowired
    private AccessValidator accessValidator;
    @Autowired
    private EntityService entityService;

    @ApiOperation(value = "Push user message with ACL enrichment to the rule engine (handleRuleEngineRequestV2)",
            notes = V2_DESCRIPTION + "\n\nUses the originator from the request body, or the calling User id when omitted."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/v2", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequestV2(
            @Parameter(description = "Enriched request body containing `payload` and optional `aclEntities`.", required = true)
            @RequestBody RuleEngineV2Request request) throws ThingsboardException {
        if (request == null || request.getPayload() == null) {
            throw new ThingsboardException("Request body with 'payload' is required",
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        int timeout = request.getTimeout() > 0 ? request.getTimeout() : defaultResponseTimeout;
        EntityId originator = request.getOriginator();
        String entityTypeStr = originator != null ? originator.getEntityType().name() : null;
        String entityIdStr = originator != null ? originator.getId().toString() : null;
        return handleRuleEngineRequest(entityTypeStr, entityIdStr, timeout, JacksonUtil.toString(request.getPayload()), request.getQueueName(), request.getAclEntities());
    }

    @ApiOperation(value = "Push user message to the rule engine (handleRuleEngineRequestForUser)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses current User Id ( the one which credentials is used to perform the request) as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The default timeout of the request processing is 10 seconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequestForUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON object representing the message.", required = true,
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(null, null, defaultResponseTimeout, requestBody, null, null);
    }

    @ApiOperation(value = "Push entity message to the rule engine (handleRuleEngineRequestForEntity)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The default timeout of the request processing is 10 seconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequestForEntity(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON object representing the message.", required = true,
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(entityType, entityIdStr, defaultResponseTimeout, requestBody, null, null);
    }

    @ApiOperation(value = "Push entity message with timeout to the rule engine (handleRuleEngineRequestForEntityWithTimeout)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The platform expects the timeout value in milliseconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{timeout}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequestForEntityWithTimeout(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @Parameter(description = "Timeout to process the request in milliseconds", required = true)
            @PathVariable("timeout") int timeout,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON object representing the message.", required = true,
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(entityType, entityIdStr, timeout, requestBody, null, null);
    }

    @ApiOperation(value = "Push entity message with timeout and specified queue to the rule engine (handleRuleEngineRequestForEntityWithQueueAndTimeout)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "If request sent for Device/Device Profile or Asset/Asset Profile entity, specified queue will be used instead of the queue selected in the device or asset profile. " +
                    "The platform expects the timeout value in milliseconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{queueName}/{timeout}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequestForEntityWithQueueAndTimeout(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @Parameter(description = "Queue name to process the request in the rule engine", required = true)
            @PathVariable("queueName") String queueName,
            @Parameter(description = "Timeout to process the request in milliseconds", required = true)
            @PathVariable("timeout") int timeout,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "A JSON object representing the message.", required = true,
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(entityType, entityIdStr, timeout, requestBody, queueName, null);
    }

    private DeferredResult<ResponseEntity> handleRuleEngineRequest(String entityType,
                                                                   String entityIdStr,
                                                                   int timeout,
                                                                   String requestBody,
                                                                   String queueName,
                                                                   List<EntityId> aclEntities) throws ThingsboardException {
        try {
            SecurityUser currentUser = getCurrentUser();
            EntityId entityId;
            if (StringUtils.isEmpty(entityType) || StringUtils.isEmpty(entityIdStr)) {
                entityId = currentUser.getId();
            } else {
                entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            }
            if (maxAclEntities > 0 && aclEntities != null && aclEntities.size() > maxAclEntities) {
                throw new ThingsboardException(
                        "Exceeded max ACL enrichment entities: " + maxAclEntities,
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            //Check that this is a valid JSON
            JacksonUtil.toJsonNode(requestBody);
            final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            accessValidator.validate(currentUser, Operation.WRITE, entityId, new HttpValidationCallback(response, new FutureCallback<DeferredResult<ResponseEntity>>() {
                @Override
                public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                    long expTime = System.currentTimeMillis() + timeout;
                    HashMap<String, String> metaData = new HashMap<>();
                    UUID requestId = UUID.randomUUID();
                    metaData.put("serviceId", serviceInfoProvider.getServiceId());
                    metaData.put("requestUUID", requestId.toString());
                    metaData.put("expirationTime", Long.toString(expTime));
                    // Server-authoritative keys — written last so any caller value is overwritten.
                    metaData.put(TbMsgMetaData.TB_USER_ID_KEY, currentUser.getId().getId().toString());
                    metaData.put(TbMsgMetaData.TB_ACL_KEY, buildAclSnapshot(currentUser, aclEntities));

                    TbMsg msg = TbMsg.newMsg()
                            .queueName(queueName)
                            .type(TbMsgType.REST_API_REQUEST)
                            .originator(entityId)
                            .customerId(currentUser.getCustomerId())
                            .copyMetaData(new TbMsgMetaData(metaData))
                            .data(requestBody)
                            .build();
                    ruleEngineCallService.processRestApiCallToRuleEngine(currentUser.getTenantId(), requestId, msg, queueName != null,
                            reply -> reply(new LocalRequestMetaData(msg, currentUser, result), reply));
                }

                @Override
                public void onFailure(Throwable e) {
                    ResponseEntity entity;
                    if (e instanceof ToErrorResponseEntity) {
                        entity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
                    } else {
                        entity = new ResponseEntity(HttpStatus.UNAUTHORIZED);
                    }
                    logRuleEngineCall(currentUser, entityId, requestBody, null, e);
                    response.setResult(entity);
                }
            }));
            return response;
        } catch (IllegalArgumentException iae) {
            throw new ThingsboardException("Invalid request body", iae, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * Computes the ACL snapshot for the requested entities under the given user.
     * For each entity, the target is loaded via
     * {@link EntityService#fetchEntity(org.thingsboard.server.common.data.id.TenantId, EntityId)},
     * the target {@link Resource} is resolved via {@link Resource#of(org.thingsboard.server.common.data.EntityType)},
     * then every {@link Operation} value is probed via
     * {@link org.thingsboard.server.service.security.permission.AccessControlService#hasPermission(SecurityUser, Resource, Operation, EntityId, HasTenantId)}
     * so that ownership, customer hierarchy, and group membership of the specific
     * instance are taken into account. Operations returning {@code true} are accumulated
     * into the entry. Entries with unmapped EntityTypes, missing entities, or entities
     * that are not {@link HasTenantId} produce {@code allowed=[]}.
     *
     * <p>Note: a {@code SYS_ADMIN} caller operates against the system tenant, so tenant-scoped
     * entities (DEVICE, ASSET, ...) won't be resolved by the tenant-filtered lookup and the
     * entry resolves to {@code allowed=[]} — ACL enrichment is effectively a no-op for SYS_ADMIN.
     *
     * @return serialized JSON array suitable for writing into {@link TbMsgMetaData#TB_ACL_KEY}.
     */
    String buildAclSnapshot(SecurityUser user, List<EntityId> entities) {
        if (entities == null || entities.isEmpty()) {
            return "[]";
        }
        List<EntityAclEntry> result = new ArrayList<>(entities.size());
        for (EntityId entityId : entities) {
            Set<String> allowed = new LinkedHashSet<>();
            Resource resource;
            try {
                resource = Resource.of(entityId.getEntityType());
            } catch (IllegalArgumentException e) {
                result.add(new EntityAclEntry(entityId, allowed));
                continue;
            }
            HasId<?> entity = entityService.fetchEntity(user.getTenantId(), entityId).orElse(null);
            if (!(entity instanceof HasTenantId tenantEntity)) {
                result.add(new EntityAclEntry(entityId, allowed));
                continue;
            }
            for (Operation op : EnumSet.allOf(Operation.class)) {
                try {
                    if (accessControlService.hasPermission(user, resource, op, entityId, tenantEntity)) {
                        allowed.add(op.name());
                    }
                } catch (ThingsboardException e) {
                    log.debug("[{}] ACL probe failed for {} {}: {}", user.getId(), op, entityId, e.getMessage());
                }
            }
            result.add(new EntityAclEntry(entityId, allowed));
        }
        return JacksonUtil.toString(result);
    }

    private void reply(LocalRequestMetaData rpcRequest, TbMsg response) {
        DeferredResult<ResponseEntity> responseWriter = rpcRequest.responseWriter();
        if (response == null) {
            logRuleEngineCall(rpcRequest, null, new TimeoutException("Processing timeout detected!"));
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT));
        } else {
            String responseData = response.getData();
            if (!StringUtils.isEmpty(responseData)) {
                try {
                    logRuleEngineCall(rpcRequest, response, null);
                    responseWriter.setResult(new ResponseEntity<>(JacksonUtil.toJsonNode(responseData), HttpStatus.OK));
                } catch (IllegalArgumentException e) {
                    log.debug("Failed to decode device response: {}", responseData, e);
                    logRuleEngineCall(rpcRequest, response, e);
                    responseWriter.setResult(new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE));
                }
            } else {
                logRuleEngineCall(rpcRequest, response, null);
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.OK));
            }
        }
    }

    private void logRuleEngineCall(LocalRequestMetaData rpcRequest, TbMsg response, Throwable e) {
        logRuleEngineCall(rpcRequest.user(), rpcRequest.request().getOriginator(), rpcRequest.request().getData(), response, e);
    }

    private void logRuleEngineCall(SecurityUser user, EntityId entityId, String request, TbMsg response, Throwable e) {
        auditLogService.logEntityAction(
                user.getTenantId(),
                user.getCustomerId(),
                user.getId(),
                user.getName(),
                entityId,
                null,
                ActionType.REST_API_RULE_ENGINE_CALL,
                BaseController.toException(e),
                request,
                response != null ? response.getData() : "");
    }

    private record LocalRequestMetaData(TbMsg request, SecurityUser user,
                                        DeferredResult<ResponseEntity> responseWriter) {
    }
}
