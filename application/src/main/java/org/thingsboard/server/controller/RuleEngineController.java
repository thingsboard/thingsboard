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
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ruleengine.RuleEngineCallService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;

@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.RULE_ENGINE_URL_PREFIX)
@Slf4j
public class RuleEngineController extends BaseController {
    public static final int DEFAULT_TIMEOUT = 10000;
    private static final String MSG_DESCRIPTION_PREFIX = "Creates the Message with type 'REST_API_REQUEST' and payload taken from the request body. ";
    private static final String MSG_DESCRIPTION = "This method allows you to extend the regular platform API with the power of Rule Engine. You may use default and custom rule nodes to handle the message. " +
            "The generated message contains two important metadata fields:\n\n" +
            " * **'serviceId'** to identify the platform server that received the request;\n" +
            " * **'requestUUID'** to identify the request and route possible response from the Rule Engine;\n\n" +
            "Use **'rest call reply'** rule node to push the reply from rule engine back as a REST API call response. ";

    @Autowired
    private RuleEngineCallService ruleEngineCallService;
    @Autowired
    private AccessValidator accessValidator;

    @ApiOperation(value = "Push user message to the rule engine (handleRuleEngineRequest)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses current User Id ( the one which credentials is used to perform the request) as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The default timeout of the request processing is 10 seconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequest(
            @Parameter(description = "A JSON value representing the message.", required = true)
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(null, null, null, DEFAULT_TIMEOUT, requestBody);
    }

    @ApiOperation(value = "Push entity message to the rule engine (handleRuleEngineRequest)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The default timeout of the request processing is 10 seconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequest(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @Parameter(description = "A JSON value representing the message.", required = true)
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(entityType, entityIdStr, null, DEFAULT_TIMEOUT, requestBody);
    }

    @ApiOperation(value = "Push entity message with timeout to the rule engine (handleRuleEngineRequest)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "The platform expects the timeout value in milliseconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{timeout}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequest(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @Parameter(description = "Timeout to process the request in milliseconds", required = true)
            @PathVariable("timeout") int timeout,
            @Parameter(description = "A JSON value representing the message.", required = true)
            @RequestBody String requestBody) throws ThingsboardException {
        return handleRuleEngineRequest(entityType, entityIdStr, null, timeout, requestBody);
    }

    @ApiOperation(value = "Push entity message with timeout and specified queue to the rule engine (handleRuleEngineRequest)",
            notes = MSG_DESCRIPTION_PREFIX +
                    "Uses specified Entity Id as the Rule Engine message originator. " +
                    MSG_DESCRIPTION +
                    "If request sent for Device/Device Profile or Asset/Asset Profile entity, specified queue will be used instead of the queue selected in the device or asset profile. " +
                    "The platform expects the timeout value in milliseconds."
                    + "\n\n" + ControllerConstants.SECURITY_WRITE_CHECK)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{queueName}/{timeout}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> handleRuleEngineRequest(
            @Parameter(description = ENTITY_TYPE_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityType") String entityType,
            @Parameter(description = ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("entityId") String entityIdStr,
            @Parameter(description = "Queue name to process the request in the rule engine", required = true)
            @PathVariable("queueName") String queueName,
            @Parameter(description = "Timeout to process the request in milliseconds", required = true)
            @PathVariable("timeout") int timeout,
            @Parameter(description = "A JSON value representing the message.", required = true)
            @RequestBody String requestBody) throws ThingsboardException {
        try {
            SecurityUser currentUser = getCurrentUser();
            EntityId entityId;
            if (StringUtils.isEmpty(entityType) || StringUtils.isEmpty(entityIdStr)) {
                entityId = currentUser.getId();
            } else {
                entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
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

    private record LocalRequestMetaData(TbMsg request, SecurityUser user, DeferredResult<ResponseEntity> responseWriter) {}
}
