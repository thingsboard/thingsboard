/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.api.msg.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseDeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.telemetry.AttributeData;
import org.thingsboard.server.service.telemetry.TsData;
import org.thingsboard.server.service.telemetry.exception.InvalidParametersException;
import org.thingsboard.server.service.telemetry.exception.UncheckedApiException;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_KEYS_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SAVE_SCOPE_ALLOWED_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_ALLOWED_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTES_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ATTRIBUTE_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_GET_ATTRIBUTE_SCOPES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_SAVE_ATTRIBUTE_SCOPES;
import static org.thingsboard.server.controller.ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INVALID_STRUCTURE_OF_THE_REQUEST;
import static org.thingsboard.server.controller.ControllerConstants.LATEST_TS_NON_STRICT_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.LATEST_TS_STRICT_DATA_EXAMPLE;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_END;
import static org.thingsboard.server.controller.ControllerConstants.MARKDOWN_CODE_BLOCK_START;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTIRIBUTES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ATTRIBUTES_REQUEST_PAYLOAD;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_OK;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED;
import static org.thingsboard.server.controller.ControllerConstants.SAVE_TIMESERIES_REQUEST_PAYLOAD;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.STRICT_DATA_TYPES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_JSON_REQUEST_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_KEYS_BASE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_KEYS_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TELEMETRY_SCOPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TS_STRICT_DATA_EXAMPLE;


/**
 * Created by ashvayka on 22.03.18.
 */
@RestController
@TbCoreComponent
@RequestMapping(TbUrlConstants.TELEMETRY_URL_PREFIX)
@Slf4j
public class TelemetryController extends BaseController {

    @Autowired
    private TimeseriesService tsService;

    @Autowired
    private AccessValidator accessValidator;

    @Value("${transport.json.max_string_value_length:0}")
    private int maxStringValueLength;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("telemetry-controller"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @ApiOperation(value = "Get all attribute keys (getAttributeKeys)",
            notes = "Returns a set of unique attribute key names for the selected entity. " +
                    "The response will include merged key names set for all attribute scopes:" +
                    "\n\n * SERVER_SCOPE - supported for all entity types;" +
                    "\n * CLIENT_SCOPE - supported for devices;" +
                    "\n * SHARED_SCOPE - supported for devices. "
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributeKeys(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        try {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr, this::getAttributeKeysCallback);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get all attribute keys by scope (getAttributeKeysByScope)",
            notes = "Returns a set of unique attribute key names for the selected entity and attributes scope: " +
                    "\n\n * SERVER_SCOPE - supported for all entity types;" +
                    "\n * CLIENT_SCOPE - supported for devices;" +
                    "\n * SHARED_SCOPE - supported for devices. "
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/attributes/{scope}", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributeKeysByScope(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, required = true, allowableValues = ATTRIBUTES_SCOPE_ALLOWED_VALUES) @PathVariable("scope") String scope) throws ThingsboardException {
        try {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getAttributeKeysCallback(result, tenantId, entityId, scope));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get attributes (getAttributes)",
            notes = "Returns all attributes that belong to specified entity. Use optional 'keys' parameter to return specific attributes."
                    + "\n Example of the result: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/attributes", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributes(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getAttributeValuesCallback(result, user, entityId, null, keysStr));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Get attributes by scope (getAttributesByScope)",
            notes = "Returns all attributes of a specified scope that belong to specified entity." +
                    ENTITY_GET_ATTRIBUTE_SCOPES +
                    "Use optional 'keys' parameter to return specific attributes."
                    + "\n Example of the result: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + ATTRIBUTE_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/attributes/{scope}", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getAttributesByScope(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SCOPE_ALLOWED_VALUES, required = true) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr) throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_ATTRIBUTES, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getAttributeValuesCallback(result, user, entityId, scope, keysStr));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get time-series keys (getTimeseriesKeys)",
            notes = "Returns a set of unique time-series key names for the selected entity. " +
                    "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeseriesKeys(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr) throws ThingsboardException {
        try {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> Futures.addCallback(tsService.findAllLatest(tenantId, entityId), getTsKeysToResponseCallback(result), MoreExecutors.directExecutor()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get latest time-series value (getLatestTimeseries)",
            notes = "Returns all time-series that belong to specified entity. Use optional 'keys' parameter to return specific time-series." +
                    " The result is a JSON object. The format of the values depends on the 'useStrictDataTypes' parameter." +
                    " By default, all time-series values are converted to strings: \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_NON_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n However, it is possible to request the values without conversion ('useStrictDataTypes'=true): \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + LATEST_TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n " + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/timeseries", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ResponseEntity> getLatestTimeseries(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_KEYS_DESCRIPTION) @RequestParam(name = "keys", required = false) String keysStr,
            @ApiParam(value = STRICT_DATA_TYPES_DESCRIPTION)
            @RequestParam(name = "useStrictDataTypes", required = false, defaultValue = "false") Boolean useStrictDataTypes) throws ThingsboardException {
        try {
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> getLatestTimeseriesValuesCallback(result, user, entityId, keysStr, useStrictDataTypes));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get time-series data (getTimeseries)",
            notes = "Returns a range of time-series values for specified entity. " +
                    "Returns not aggregated data by default. " +
                    "Use aggregation function ('agg') and aggregation interval ('interval') to enable aggregation of the results on the database / server side. " +
                    "The aggregation is generally more efficient then fetching all records. \n\n"
                    + MARKDOWN_CODE_BLOCK_START
                    + TS_STRICT_DATA_EXAMPLE
                    + MARKDOWN_CODE_BLOCK_END
                    + "\n\n" + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/values/timeseries", method = RequestMethod.GET, params = {"keys", "startTs", "endTs"})
    @ResponseBody
    public DeferredResult<ResponseEntity> getTimeseries(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_KEYS_BASE_DESCRIPTION, required = true) @RequestParam(name = "keys") String keys,
            @ApiParam(value = "A long value representing the start timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "startTs") Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of the time range in milliseconds, UTC.")
            @RequestParam(name = "endTs") Long endTs,
            @ApiParam(value = "A long value representing the aggregation interval range in milliseconds.")
            @RequestParam(name = "interval", defaultValue = "0") Long interval,
            @ApiParam(value = "An integer value that represents a max number of timeseries data points to fetch." +
                    " This parameter is used only in the case if 'agg' parameter is set to 'NONE'.", defaultValue = "100")
            @RequestParam(name = "limit", defaultValue = "100") Integer limit,
            @ApiParam(value = "A string value representing the aggregation function. " +
                    "If the interval is not specified, 'agg' parameter will use 'NONE' value.",
                    allowableValues = "MIN, MAX, AVG, SUM, COUNT, NONE")
            @RequestParam(name = "agg", defaultValue = "NONE") String aggStr,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(name = "orderBy", defaultValue = "DESC") String orderBy,
            @ApiParam(value = STRICT_DATA_TYPES_DESCRIPTION)
            @RequestParam(name = "useStrictDataTypes", required = false, defaultValue = "false") Boolean useStrictDataTypes) throws ThingsboardException {
        try {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.READ_TELEMETRY, entityType, entityIdStr,
                    (result, tenantId, entityId) -> {
                        // If interval is 0, convert this to a NONE aggregation, which is probably what the user really wanted
                        Aggregation agg = interval == 0L ? Aggregation.valueOf(Aggregation.NONE.name()) : Aggregation.valueOf(aggStr);
                        List<ReadTsKvQuery> queries = toKeysList(keys).stream().map(key -> new BaseReadTsKvQuery(key, startTs, endTs, interval, limit, agg, orderBy))
                                .collect(Collectors.toList());

                        Futures.addCallback(tsService.findAll(tenantId, entityId, queries), getTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
                    });
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save device attributes (saveDeviceAttributes)",
            notes = "Creates or updates the device attributes based on device id and specified attribute scope. " +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTIRIBUTES_STATUS_OK +
                    "Platform creates an audit log event about device attributes updates with action type 'ATTRIBUTES_UPDATED', " +
                    "and also sends event msg to the rule engine with msg type 'ATTRIBUTES_UPDATED'."),
            @ApiResponse(code = 400, message = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = "User is not authorized to save device attributes for selected device. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 500, message = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about device attributes updates with action type 'ATTRIBUTES_UPDATED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{deviceId}/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveDeviceAttributes(
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION, required = true) @PathVariable("deviceId") String deviceIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SAVE_SCOPE_ALLOWED_VALUES, required = true) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody JsonNode request) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
            return saveAttributes(getTenantId(), entityId, scope, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save entity attributes (saveEntityAttributesV1)",
            notes = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
                    ENTITY_SAVE_ATTRIBUTE_SCOPES +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTIRIBUTES_STATUS_OK + SAVE_ENTITY_ATTRIBUTES_STATUS_OK),
            @ApiResponse(code = 400, message = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED),
            @ApiResponse(code = 500, message = SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityAttributesV1(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SAVE_SCOPE_ALLOWED_VALUES) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody JsonNode request) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return saveAttributes(getTenantId(), entityId, scope, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save entity attributes (saveEntityAttributesV2)",
            notes = "Creates or updates the entity attributes based on Entity Id and the specified attribute scope. " +
                    ENTITY_SAVE_ATTRIBUTE_SCOPES +
                    SAVE_ATTRIBUTES_REQUEST_PAYLOAD
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ATTIRIBUTES_STATUS_OK + SAVE_ENTITY_ATTRIBUTES_STATUS_OK),
            @ApiResponse(code = 400, message = SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST),
            @ApiResponse(code = 401, message = SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED),
            @ApiResponse(code = 500, message = SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/attributes/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityAttributesV2(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SAVE_SCOPE_ALLOWED_VALUES, required = true) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody JsonNode request) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return saveAttributes(getTenantId(), entityId, scope, request);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Save or update time-series data (saveEntityTelemetry)",
            notes = "Creates or updates the entity time-series data based on the Entity Id and request payload." +
                    SAVE_TIMESERIES_REQUEST_PAYLOAD +
                    "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ENTITY_TIMESERIES_STATUS_OK),
            @ApiResponse(code = 400, message = INVALID_STRUCTURE_OF_THE_REQUEST),
            @ApiResponse(code = 401, message = SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED),
            @ApiResponse(code = 500, message = SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/timeseries/{scope}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityTelemetry(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_SCOPE_DESCRIPTION, required = true, allowableValues = "ANY") @PathVariable("scope") String scope,
            @ApiParam(value = TELEMETRY_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody String requestBody) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return saveTelemetry(getTenantId(), entityId, requestBody, 0L);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save or update time-series data with TTL (saveEntityTelemetryWithTTL)",
            notes = "Creates or updates the entity time-series data based on the Entity Id and request payload." +
                    SAVE_TIMESERIES_REQUEST_PAYLOAD +
                    "\n\n The scope parameter is not used in the API call implementation but should be specified whatever value because it is used as a path variable. "
                    + "\n\nThe ttl parameter takes affect only in case of Cassandra DB."
                    + INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SAVE_ENTITY_TIMESERIES_STATUS_OK),
            @ApiResponse(code = 400, message = INVALID_STRUCTURE_OF_THE_REQUEST),
            @ApiResponse(code = 401, message = SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED),
            @ApiResponse(code = 500, message = SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/timeseries/{scope}/{ttl}", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> saveEntityTelemetryWithTTL(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_SCOPE_DESCRIPTION, required = true, allowableValues = "ANY") @PathVariable("scope") String scope,
            @ApiParam(value = "A long value representing TTL (Time to Live) parameter.", required = true) @PathVariable("ttl") Long ttl,
            @ApiParam(value = TELEMETRY_JSON_REQUEST_DESCRIPTION, required = true) @RequestBody String requestBody) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return saveTelemetry(getTenantId(), entityId, requestBody, ttl);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete entity time-series data (deleteEntityTimeseries)",
            notes = "Delete time-series for selected entity based on entity id, entity type and keys." +
                    " Use 'deleteAllDataForKeys' to delete all time-series data." +
                    " Use 'startTs' and 'endTs' to specify time-range instead. " +
                    " Use 'rewriteLatestIfDeleted' to rewrite latest value (stored in separate table for performance) after deletion of the time range. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Timeseries for the selected keys in the request was removed. " +
                    "Platform creates an audit log event about entity timeseries removal with action type 'TIMESERIES_DELETED'."),
            @ApiResponse(code = 400, message = "Platform returns a bad request in case if keys list is empty or start and end timestamp values is empty when deleteAllDataForKeys is set to false."),
            @ApiResponse(code = 401, message = "User is not authorized to delete entity timeseries for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 500, message = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about entity timeseries removal with action type 'TIMESERIES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/timeseries/delete", method = RequestMethod.DELETE)
    @ResponseBody
    public DeferredResult<ResponseEntity> deleteEntityTimeseries(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = TELEMETRY_KEYS_DESCRIPTION, required = true) @RequestParam(name = "keys") String keysStr,
            @ApiParam(value = "A boolean value to specify if should be deleted all data for selected keys or only data that are in the selected time range.")
            @RequestParam(name = "deleteAllDataForKeys", defaultValue = "false") boolean deleteAllDataForKeys,
            @ApiParam(value = "A long value representing the start timestamp of removal time range in milliseconds.")
            @RequestParam(name = "startTs", required = false) Long startTs,
            @ApiParam(value = "A long value representing the end timestamp of removal time range in milliseconds.")
            @RequestParam(name = "endTs", required = false) Long endTs,
            @ApiParam(value = "If the parameter is set to true, the latest telemetry will be rewritten in case that current latest value was removed, otherwise, in case that parameter is set to false the new latest value will not set.")
            @RequestParam(name = "rewriteLatestIfDeleted", defaultValue = "false") boolean rewriteLatestIfDeleted) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return deleteTimeseries(entityId, keysStr, deleteAllDataForKeys, startTs, endTs, rewriteLatestIfDeleted);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private DeferredResult<ResponseEntity> deleteTimeseries(EntityId entityIdStr, String keysStr, boolean deleteAllDataForKeys,
                                                            Long startTs, Long endTs, boolean rewriteLatestIfDeleted) throws ThingsboardException {
        List<String> keys = toKeysList(keysStr);
        if (keys.isEmpty()) {
            return getImmediateDeferredResult("Empty keys: " + keysStr, HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();

        long deleteFromTs;
        long deleteToTs;
        if (deleteAllDataForKeys) {
            deleteFromTs = 0L;
            deleteToTs = System.currentTimeMillis();
        } else {
            if (startTs == null || endTs == null) {
                return getImmediateDeferredResult("When deleteAllDataForKeys is false, start and end timestamp values shouldn't be empty", HttpStatus.BAD_REQUEST);
            } else {
                deleteFromTs = startTs;
                deleteToTs = endTs;
            }
        }

        return accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entityIdStr, (result, tenantId, entityId) -> {
            List<DeleteTsKvQuery> deleteTsKvQueries = new ArrayList<>();
            for (String key : keys) {
                deleteTsKvQueries.add(new BaseDeleteTsKvQuery(key, deleteFromTs, deleteToTs, rewriteLatestIfDeleted));
            }
            tsSubService.deleteTimeseriesAndNotify(tenantId, entityId, keys, deleteTsKvQueries, new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    logTimeseriesDeleted(user, entityId, keys, deleteFromTs, deleteToTs, null);
                    result.setResult(new ResponseEntity<>(HttpStatus.OK));
                }

                @Override
                public void onFailure(Throwable t) {
                    logTimeseriesDeleted(user, entityId, keys, deleteFromTs, deleteToTs, t);
                    result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                }
            });
        });
    }

    @ApiOperation(value = "Delete device attributes (deleteDeviceAttributes)",
            notes = "Delete device attributes using provided Device Id, scope and a list of keys. " +
                    "Referencing a non-existing Device Id will cause an error" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Device attributes was removed for the selected keys in the request. " +
                    "Platform creates an audit log event about device attributes removal with action type 'ATTRIBUTES_DELETED'."),
            @ApiResponse(code = 400, message = "Platform returns a bad request in case if keys or scope are not specified."),
            @ApiResponse(code = 401, message = "User is not authorized to delete device attributes for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 500, message = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about device attributes removal with action type 'ATTRIBUTES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{deviceId}/{scope}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeferredResult<ResponseEntity> deleteDeviceAttributes(
            @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION, required = true) @PathVariable(DEVICE_ID) String deviceIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, allowableValues = ATTRIBUTES_SCOPE_ALLOWED_VALUES, required = true) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_KEYS_DESCRIPTION, required = true) @RequestParam(name = "keys") String keysStr) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE, deviceIdStr);
            return deleteAttributes(entityId, scope, keysStr);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete entity attributes (deleteEntityAttributes)",
            notes = "Delete entity attributes using provided Entity Id, scope and a list of keys. " +
                    INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Entity attributes was removed for the selected keys in the request. " +
                    "Platform creates an audit log event about entity attributes removal with action type 'ATTRIBUTES_DELETED'."),
            @ApiResponse(code = 400, message = "Platform returns a bad request in case if keys or scope are not specified."),
            @ApiResponse(code = 401, message = "User is not authorized to delete entity attributes for selected entity. Most likely, User belongs to different Customer or Tenant."),
            @ApiResponse(code = 500, message = "The exception was thrown during processing the request. " +
                    "Platform creates an audit log event about entity attributes removal with action type 'ATTRIBUTES_DELETED' that includes an error stacktrace."),
    })
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/{entityType}/{entityId}/{scope}", method = RequestMethod.DELETE)
    @ResponseBody
    public DeferredResult<ResponseEntity> deleteEntityAttributes(
            @ApiParam(value = ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE") @PathVariable("entityType") String entityType,
            @ApiParam(value = ENTITY_ID_PARAM_DESCRIPTION, required = true) @PathVariable("entityId") String entityIdStr,
            @ApiParam(value = ATTRIBUTES_SCOPE_DESCRIPTION, required = true, allowableValues = ATTRIBUTES_SCOPE_ALLOWED_VALUES) @PathVariable("scope") String scope,
            @ApiParam(value = ATTRIBUTES_KEYS_DESCRIPTION, required = true) @RequestParam(name = "keys") String keysStr) throws ThingsboardException {
        try {
            EntityId entityId = EntityIdFactory.getByTypeAndId(entityType, entityIdStr);
            return deleteAttributes(entityId, scope, keysStr);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private DeferredResult<ResponseEntity> deleteAttributes(EntityId entityIdSrc, String scope, String keysStr) throws ThingsboardException {
        List<String> keys = toKeysList(keysStr);
        if (keys.isEmpty()) {
            return getImmediateDeferredResult("Empty keys: " + keysStr, HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();

        if (DataConstants.SERVER_SCOPE.equals(scope) ||
                DataConstants.SHARED_SCOPE.equals(scope) ||
                DataConstants.CLIENT_SCOPE.equals(scope)) {
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_ATTRIBUTES, entityIdSrc, (result, tenantId, entityId) -> {
                tsSubService.deleteAndNotify(tenantId, entityId, scope, keys, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        logAttributesDeleted(user, entityId, scope, keys, null);
                        if (entityIdSrc.getEntityType().equals(EntityType.DEVICE)) {
                            DeviceId deviceId = new DeviceId(entityId.getId());
                            tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(
                                    user.getTenantId(), deviceId, scope, keys), null);
                        }
                        result.setResult(new ResponseEntity<>(HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        logAttributesDeleted(user, entityId, scope, keys, t);
                        result.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                    }
                });
            });
        } else {
            return getImmediateDeferredResult("Invalid attribute scope: " + scope, HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity> saveAttributes(TenantId srcTenantId, EntityId entityIdSrc, String scope, JsonNode json) throws ThingsboardException {
        if (!DataConstants.SERVER_SCOPE.equals(scope) && !DataConstants.SHARED_SCOPE.equals(scope)) {
            return getImmediateDeferredResult("Invalid scope: " + scope, HttpStatus.BAD_REQUEST);
        }
        if (json.isObject()) {
            List<AttributeKvEntry> attributes = extractRequestAttributes(json);
            attributes.forEach(ConstraintValidator::validateFields);
            if (attributes.isEmpty()) {
                return getImmediateDeferredResult("No attributes data found in request body!", HttpStatus.BAD_REQUEST);
            }
            for (AttributeKvEntry attributeKvEntry : attributes) {
                if (attributeKvEntry.getKey().isEmpty() || attributeKvEntry.getKey().trim().length() == 0) {
                    return getImmediateDeferredResult("Key cannot be empty or contains only spaces", HttpStatus.BAD_REQUEST);
                }
            }
            SecurityUser user = getCurrentUser();
            return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_ATTRIBUTES, entityIdSrc, (result, tenantId, entityId) -> {
                tsSubService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        logAttributesUpdated(user, entityId, scope, attributes, null);
                        result.setResult(new ResponseEntity(HttpStatus.OK));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        logAttributesUpdated(user, entityId, scope, attributes, t);
                        AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                });
            });
        } else {
            return getImmediateDeferredResult("Request is not a JSON object", HttpStatus.BAD_REQUEST);
        }
    }

    private DeferredResult<ResponseEntity> saveTelemetry(TenantId curTenantId, EntityId entityIdSrc, String requestBody, long ttl) throws ThingsboardException {
        Map<Long, List<KvEntry>> telemetryRequest;
        JsonElement telemetryJson;
        try {
            telemetryJson = new JsonParser().parse(requestBody);
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse timeseries payload: Invalid JSON body!", HttpStatus.BAD_REQUEST);
        }
        try {
            telemetryRequest = JsonConverter.convertToTelemetry(telemetryJson, System.currentTimeMillis());
        } catch (Exception e) {
            return getImmediateDeferredResult("Unable to parse timeseries payload. Invalid JSON body: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        List<TsKvEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> entry : telemetryRequest.entrySet()) {
            for (KvEntry kv : entry.getValue()) {
                entries.add(new BasicTsKvEntry(entry.getKey(), kv));
            }
        }
        if (entries.isEmpty()) {
            return getImmediateDeferredResult("No timeseries data found in request body!", HttpStatus.BAD_REQUEST);
        }
        SecurityUser user = getCurrentUser();
        return accessValidator.validateEntityAndCallback(getCurrentUser(), Operation.WRITE_TELEMETRY, entityIdSrc, (result, tenantId, entityId) -> {
            long tenantTtl = ttl;
            if (!TenantId.SYS_TENANT_ID.equals(tenantId) && tenantTtl == 0) {
                TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
                tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            }
            tsSubService.saveAndNotify(tenantId, user.getCustomerId(), entityId, entries, tenantTtl, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    logTelemetryUpdated(user, entityId, entries, null);
                    result.setResult(new ResponseEntity(HttpStatus.OK));
                }

                @Override
                public void onFailure(Throwable t) {
                    logTelemetryUpdated(user, entityId, entries, t);
                    AccessValidator.handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            });
        });
    }

    private void getLatestTimeseriesValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String keys, Boolean useStrictDataTypes) {
        ListenableFuture<List<TsKvEntry>> future;
        if (StringUtils.isEmpty(keys)) {
            future = tsService.findAllLatest(user.getTenantId(), entityId);
        } else {
            future = tsService.findLatest(user.getTenantId(), entityId, toKeysList(keys));
        }
        Futures.addCallback(future, getTsKvListCallback(result, useStrictDataTypes), MoreExecutors.directExecutor());
    }

    private void getAttributeValuesCallback(@Nullable DeferredResult<ResponseEntity> result, SecurityUser user, EntityId entityId, String scope, String keys) {
        List<String> keyList = toKeysList(keys);
        FutureCallback<List<AttributeKvEntry>> callback = getAttributeValuesToResponseCallback(result, user, scope, entityId, keyList);
        if (!StringUtils.isEmpty(scope)) {
            if (keyList != null && !keyList.isEmpty()) {
                Futures.addCallback(attributesService.find(user.getTenantId(), entityId, scope, keyList), callback, MoreExecutors.directExecutor());
            } else {
                Futures.addCallback(attributesService.findAll(user.getTenantId(), entityId, scope), callback, MoreExecutors.directExecutor());
            }
        } else {
            List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
            for (String tmpScope : DataConstants.allScopes()) {
                if (keyList != null && !keyList.isEmpty()) {
                    futures.add(attributesService.find(user.getTenantId(), entityId, tmpScope, keyList));
                } else {
                    futures.add(attributesService.findAll(user.getTenantId(), entityId, tmpScope));
                }
            }

            ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, TenantId tenantId, EntityId entityId, String scope) {
        Futures.addCallback(attributesService.findAll(tenantId, entityId, scope), getAttributeKeysToResponseCallback(result), MoreExecutors.directExecutor());
    }

    private void getAttributeKeysCallback(@Nullable DeferredResult<ResponseEntity> result, TenantId tenantId, EntityId entityId) {
        List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
        for (String scope : DataConstants.allScopes()) {
            futures.add(attributesService.findAll(tenantId, entityId, scope));
        }

        ListenableFuture<List<AttributeKvEntry>> future = mergeAllAttributesFutures(futures);

        Futures.addCallback(future, getAttributeKeysToResponseCallback(result), MoreExecutors.directExecutor());
    }

    private FutureCallback<List<TsKvEntry>> getTsKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> values) {
                List<String> keys = values.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeKeysToResponseCallback(final DeferredResult<ResponseEntity> response) {
        return new FutureCallback<List<AttributeKvEntry>>() {

            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<String> keys = attributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
                response.setResult(new ResponseEntity<>(keys, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<AttributeKvEntry>> getAttributeValuesToResponseCallback(final DeferredResult<ResponseEntity> response,
                                                                                        final SecurityUser user, final String scope,
                                                                                        final EntityId entityId, final List<String> keyList) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> attributes) {
                List<AttributeData> values = attributes.stream().map(attribute ->
                        new AttributeData(attribute.getLastUpdateTs(), attribute.getKey(), getKvValue(attribute))
                ).collect(Collectors.toList());
                logAttributesRead(user, entityId, scope, keyList, null);
                response.setResult(new ResponseEntity<>(values, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch attributes", e);
                logAttributesRead(user, entityId, scope, keyList, e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private FutureCallback<List<TsKvEntry>> getTsKvListCallback(final DeferredResult<ResponseEntity> response, Boolean useStrictDataTypes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(List<TsKvEntry> data) {
                Map<String, List<TsData>> result = new LinkedHashMap<>();
                for (TsKvEntry entry : data) {
                    Object value = useStrictDataTypes ? getKvValue(entry) : entry.getValueAsString();
                    result.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(new TsData(entry.getTs(), value));
                }
                response.setResult(new ResponseEntity<>(result, HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable e) {
                log.error("Failed to fetch historical data", e);
                AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private void logTimeseriesDeleted(SecurityUser user, EntityId entityId, List<String> keys, long startTs, long endTs, Throwable e) {
        notificationEntityService.logEntityAction(user.getTenantId(), entityId, ActionType.TIMESERIES_DELETED, user,
                toException(e), keys, startTs, endTs);
    }

    private void logTelemetryUpdated(SecurityUser user, EntityId entityId, List<TsKvEntry> telemetry, Throwable e) {
        notificationEntityService.logEntityAction(user.getTenantId(), entityId, ActionType.TIMESERIES_UPDATED, user,
                toException(e), telemetry);
    }

    private void logAttributesDeleted(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) {
        notificationEntityService.logEntityAction(user.getTenantId(), (UUIDBased & EntityId) entityId,
                ActionType.ATTRIBUTES_DELETED, user, toException(e), scope, keys);
    }

    private void logAttributesUpdated(SecurityUser user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) {
        notificationEntityService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_UPDATED, user,
                toException(e), scope, attributes);
    }


    private void logAttributesRead(SecurityUser user, EntityId entityId, String scope, List<String> keys, Throwable e) {
        notificationEntityService.logEntityAction(user.getTenantId(), entityId, ActionType.ATTRIBUTES_READ, user,
                toException(e), scope, keys);
    }

    private ListenableFuture<List<AttributeKvEntry>> mergeAllAttributesFutures(List<ListenableFuture<List<AttributeKvEntry>>> futures) {
        return Futures.transform(Futures.successfulAsList(futures),
                (Function<? super List<List<AttributeKvEntry>>, ? extends List<AttributeKvEntry>>) input -> {
                    List<AttributeKvEntry> tmp = new ArrayList<>();
                    if (input != null) {
                        input.forEach(tmp::addAll);
                    }
                    return tmp;
                }, executor);
    }

    private List<String> toKeysList(String keys) {
        List<String> keyList = null;
        if (!StringUtils.isEmpty(keys)) {
            keyList = Arrays.asList(keys.split(","));
        }
        return keyList;
    }

    private DeferredResult<ResponseEntity> getImmediateDeferredResult(String message, HttpStatus status) {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();
        result.setResult(new ResponseEntity<>(message, status));
        return result;
    }

    private List<AttributeKvEntry> extractRequestAttributes(JsonNode jsonNode) {
        long ts = System.currentTimeMillis();
        List<AttributeKvEntry> attributes = new ArrayList<>();
        jsonNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (entry.getValue().isObject() || entry.getValue().isArray()) {
                attributes.add(new BaseAttributeKvEntry(new JsonDataEntry(key, toJsonStr(value)), ts));
            } else if (entry.getValue().isTextual()) {
                if (maxStringValueLength > 0 && entry.getValue().textValue().length() > maxStringValueLength) {
                    String message = String.format("String value length [%d] for key [%s] is greater than maximum allowed [%d]", entry.getValue().textValue().length(), key, maxStringValueLength);
                    throw new UncheckedApiException(new InvalidParametersException(message));
                }
                attributes.add(new BaseAttributeKvEntry(new StringDataEntry(key, value.textValue()), ts));
            } else if (entry.getValue().isBoolean()) {
                attributes.add(new BaseAttributeKvEntry(new BooleanDataEntry(key, value.booleanValue()), ts));
            } else if (entry.getValue().isDouble()) {
                attributes.add(new BaseAttributeKvEntry(new DoubleDataEntry(key, value.doubleValue()), ts));
            } else if (entry.getValue().isNumber()) {
                if (entry.getValue().isBigInteger()) {
                    throw new UncheckedApiException(new InvalidParametersException("Big integer values are not supported!"));
                } else {
                    attributes.add(new BaseAttributeKvEntry(new LongDataEntry(key, value.longValue()), ts));
                }
            }
        });
        return attributes;
    }

    private String toJsonStr(JsonNode value) {
        try {
            return JacksonUtil.toString(value);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Can't parse jsonValue: " + value, e);
        }
    }

    private JsonNode toJsonNode(String value) {
        try {
            return JacksonUtil.toJsonNode(value);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Can't parse jsonValue: " + value, e);
        }
    }

    private Object getKvValue(KvEntry entry) {
        if (entry.getDataType() == DataType.JSON) {
            return toJsonNode(entry.getJsonValue().get());
        }
        return entry.getValue();
    }
}
