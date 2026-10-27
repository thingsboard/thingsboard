// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
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
import org.thingsboard.integration.api.converter.AbstractDownlinkDataConverter;
import org.thingsboard.integration.api.converter.DedicatedConverterConfig;
import org.thingsboard.integration.api.converter.DedicatedUplinkData;
import org.thingsboard.integration.api.converter.ScriptDownlinkEvaluator;
import org.thingsboard.integration.api.converter.ScriptUplinkEvaluator;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapperFactory;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.subscription.PlatformFeature;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.converter.TbConverterService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.integration.api.converter.DedicatedConverterUtil.parseUplinkData;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_CONFIGURATION_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_DEBUG_INPUT_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.CONVERTER_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEDICATED_CONVERTER_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_KNP_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_SIGFOX_UPLINK_CONVERTER_METADATA;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_THINGSPARK_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TPE_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE;
import static org.thingsboard.server.controller.ControllerConstants.INCLUDE_GATEWAY_INFO;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_NAME;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_NAME_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INTEGRATION_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.IS_GATEWAY_INFO_INCLUDED;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_DELETE_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.RBAC_READ_CHECK;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TEST_DOWNLINK_CONVERTER_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.TEST_UPLINK_CONVERTER_DEFINITION;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class ConverterController extends AutoCommitController {

    private final EventService eventService;
    private final JsInvokeService jsInvokeService;
    private final Optional<TbelInvokeService> tbelInvokeService;
    private final TbConverterService tbConverterService;

    private static final Map<IntegrationType, String> converterDefaultMessages = ImmutableMap.<IntegrationType, String>builder()
            .put(IntegrationType.LORIOT, DEFAULT_LORIOT_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.SIGFOX, DEFAULT_SIGFOX_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.TTI, DEFAULT_TTI_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.TTN, DEFAULT_TTN_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.CHIRPSTACK, DEFAULT_CHIRPSTACK_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.AZURE_IOT_HUB, DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.AZURE_EVENT_HUB, DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.AZURE_SERVICE_BUS, DEFAULT_AZURE_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.AWS_IOT, DEFAULT_AWS_IOT_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.KPN, DEFAULT_KNP_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.THINGPARK, DEFAULT_THINGSPARK_UPLINK_CONVERTER_MESSAGE)
            .put(IntegrationType.TPE, DEFAULT_TPE_UPLINK_CONVERTER_MESSAGE)
            .build();

    private final static Map<IntegrationType, String> converterDefaultMetadatas = Map.of(
            IntegrationType.SIGFOX, DEFAULT_SIGFOX_UPLINK_CONVERTER_METADATA
    );

    public static final String CONVERTER_ID = "converterId";

    @ApiOperation(value = "Get Converter (getConverterById)",
            notes = "Fetch the Converter object based on the provided Converter Id. " +
                    "The server checks that the converter is owned by the same tenant. "
                    + NEW_LINE + RBAC_READ_CHECK
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/converter/{converterId}")
    public Converter getConverterById(@Parameter(required = true, description = CONVERTER_ID_PARAM_DESCRIPTION)
                                      @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        checkParameter(CONVERTER_ID, strConverterId);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        return checkConverterId(converterId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Converter (saveConverter)",
            notes = "Create or update the Converter. When creating converter, platform generates Converter Id as " + UUID_WIKI_LINK +
                    "The newly created converter id will be present in the response. " +
                    "Specify existing Converter id to update the converter. " +
                    "Referencing non-existing converter Id will cause 'Not Found' error. " +
                    "Converter name is unique in the scope of tenant. " + NEW_LINE +
                    CONVERTER_CONFIGURATION_DESCRIPTION +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new converter entity. " +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/converter")
    public Converter saveConverter(@io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "A JSON value representing the converter.") @RequestBody Converter converter) throws Exception {
        checkFeatureAllowed(PlatformFeature.INTEGRATIONS);
        converter.setTenantId(getCurrentUser().getTenantId());
        checkEntity(converter.getId(), converter, Resource.CONVERTER);
        return tbConverterService.save(converter, getCurrentUser());
    }

    @ApiOperation(value = "Get Converters (getConverters)",
            notes = "Returns a page of converters owned by tenant. " +
                    PAGE_DATA_PARAMETERS + NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/converters", params = {"pageSize", "page"})
    public PageData<Converter> getConverters(
            @Parameter(description = "Fetch edge template converters")
            @RequestParam(value = "isEdgeTemplate", required = false, defaultValue = "false") boolean isEdgeTemplate,
            @Parameter(required = true, description = PAGE_SIZE_DESCRIPTION, schema = @Schema(minimum = "1"))
            @RequestParam int pageSize,
            @Parameter(required = true, description = PAGE_NUMBER_DESCRIPTION, schema = @Schema(minimum = "0"))
            @RequestParam int page,
            @Parameter(description = CONVERTER_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @Parameter(description = SORT_PROPERTY_DESCRIPTION, schema = @Schema(allowableValues = {"createdTime", "name", "type", "debugMode"}))
            @RequestParam(required = false) String sortProperty,
            @Parameter(description = SORT_ORDER_DESCRIPTION, schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false) String sortOrder,
            @Parameter(description = INTEGRATION_TYPE_DESCRIPTION)
            @RequestParam(required = false) IntegrationType integrationType) throws ThingsboardException {
        accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ);
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        if (isEdgeTemplate) {
            return checkNotNull(converterService.findTenantEdgeTemplateConverters(tenantId, integrationType, pageLink));
        } else {
            return checkNotNull(converterService.findTenantConverters(tenantId, integrationType, pageLink));
        }
    }

    @ApiOperation(value = "Delete converter (deleteConverter)",
            notes = "Deletes the converter and all the relations (from and to the converter). " +
                    "Referencing non-existing converter Id will cause an error. " +
                    "If the converter is associated with the integration, it will not be allowed for deletion." + NEW_LINE + RBAC_DELETE_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @DeleteMapping(value = "/converter/{converterId}")
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteConverter(@Parameter(required = true, description = CONVERTER_ID_PARAM_DESCRIPTION) @PathVariable(CONVERTER_ID) String strConverterId) throws ThingsboardException {
        // Deliberately unguarded: removal is the one write direction that reduces use of a withheld feature.
        // Persisted integrations keep running while it is withheld, so refusing this would leave a tenant
        // unable to stop them.
        checkParameter(CONVERTER_ID, strConverterId);
        ConverterId converterId = new ConverterId(toUUID(strConverterId));
        Converter converter = checkConverterId(converterId, Operation.DELETE);
        tbConverterService.delete(converter, getCurrentUser());
    }

    @ApiOperation(value = "Get latest debug input event (getLatestConverterDebugInput)",
            notes = "Returns a JSON object of the latest debug event representing the input message the converter processed. " + NEW_LINE +
                    CONVERTER_DEBUG_INPUT_DEFINITION +
                    NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/converter/{converterId}/debugIn")
    public JsonNode getLatestConverterDebugInput(@Parameter(description = CONVERTER_ID_PARAM_DESCRIPTION)
                                                 @PathVariable(CONVERTER_ID) String strConverterId,
                                                 @Parameter(description = CONVERTER_TYPE_DESCRIPTION)
                                                 @RequestParam(required = false) String converterType,
                                                 @Parameter(description = INTEGRATION_TYPE_DESCRIPTION)
                                                 @RequestParam(required = false) String integrationType,
                                                 @Parameter(description = INTEGRATION_NAME_PARAM_DESCRIPTION)
                                                 @RequestParam(required = false) String integrationName,
                                                 @Parameter(description = "Converter version.")
                                                 @RequestParam(required = false) Integer converterVersion) throws Exception {
        checkParameter(CONVERTER_ID, strConverterId);
        UUID uuid = toUUID(strConverterId);
        Converter converter = null;

        if (!EntityId.NULL_UUID.equals(uuid)) {
            ConverterId converterId = new ConverterId(uuid);
            converter = checkConverterId(converterId, Operation.READ);
            List<EventInfo> events = eventService.findLatestEvents(getTenantId(), converterId, EventType.DEBUG_CONVERTER, 1);

            if (events != null && !events.isEmpty()) {
                JsonNode body = events.get(0).getBody();
                return createDebugInFromFoundEvent(body);
            }
        }

        return createDefaultDebugIn(converter, converterType, integrationType, integrationName, converterVersion);
    }

    private JsonNode createDefaultDebugIn(Converter converter, String converterType, String integrationType, String integrationName, Integer converterVersion) throws ThingsboardException {
        if ((converter == null && !ConverterType.UPLINK.name().equals(converterType)) ||
                (converter != null && !ConverterType.UPLINK.equals(converter.getType()))) {
            return null;
        }

        Pair<IntegrationType, String> targetIntegrationInfo = getTargetIntegrationTypeAndName(converter, integrationType);
        if (targetIntegrationInfo == null) {
            return null;
        }

        IntegrationType targetIntegrationType = targetIntegrationInfo.getFirst();
        if (StringUtils.isBlank(integrationName)) {
            integrationName = targetIntegrationInfo.getSecond();
        }

        if (converterVersion == null) {
            converterVersion = converter != null ? converter.getConverterVersion() : null;
        }

        return createDebugIn(integrationName, targetIntegrationType, converterVersion);
    }

    private JsonNode createDebugIn(String integrationName, IntegrationType targetIntegrationType, Integer converterVersion) {
        ObjectNode debugIn = JacksonUtil.newObjectNode();
        ObjectNode metadata = JacksonUtil.newObjectNode();
        metadata.put(INTEGRATION_NAME, integrationName);
        metadata.put(INCLUDE_GATEWAY_INFO, IS_GATEWAY_INFO_INCLUDED);

        String inContent;
        ContentType contentType;
        inContent = converterDefaultMessages.get(targetIntegrationType);
        if (converterDefaultMetadatas.containsKey(targetIntegrationType)) {
            metadata.setAll(JacksonUtil.fromString(converterDefaultMetadatas.get(targetIntegrationType), ObjectNode.class));
        }
        contentType = ContentType.JSON;

        debugIn.put("inMetadata", JacksonUtil.toString(metadata));
        debugIn.put("inContent", inContent);
        debugIn.put("inContentType", contentType.name());

        return debugIn.isEmpty() ? null : debugIn;
    }

    private Pair<IntegrationType, String> getTargetIntegrationTypeAndName(Converter converter, String integrationType) throws ThingsboardException {
        IntegrationType targetIntegrationType = null;
        String targetIntegrationName = "";

        if (converter != null) {
            Integration integration = getIntegration(converter.getId());
            if (integration != null) {
                targetIntegrationType = integration.getType();
                targetIntegrationName = integration.getName();
            } else if (converter.getIntegrationType() != null) {
                targetIntegrationType = converter.getIntegrationType();
            }
        }
        if (StringUtils.isNotBlank(integrationType) && targetIntegrationType == null) {
            targetIntegrationType = IntegrationType.valueOf(integrationType);
        }

        if (targetIntegrationType != null && StringUtils.isBlank(targetIntegrationName)) {
            targetIntegrationName = "Test " + targetIntegrationType.getDirectory();
        }

        return (targetIntegrationType == null) ? null : Pair.of(targetIntegrationType, targetIntegrationName);
    }

    private Integration getIntegration(ConverterId converterId) throws ThingsboardException {
        List<Integration> relatedIntegrations = integrationService.findIntegrationsByConverterId(getTenantId(), converterId);
        if (!relatedIntegrations.isEmpty() && relatedIntegrations.stream().map(Integration::getType).distinct().limit(2).count() == 1) {
            return relatedIntegrations.get(0);
        }
        return null;
    }

    private static ObjectNode createDebugInFromFoundEvent(JsonNode body) {
        ObjectNode debugIn = JacksonUtil.newObjectNode();
        if (body.has("type")) {
            String type = body.get("type").asText();
            if (type.equals("Uplink") || type.equals("Downlink")) {
                String inContentType = body.get("inMessageType").asText();
                debugIn.put("inContentType", inContentType);
                if (type.equals("Uplink")) {
                    String inContent = body.get("in").asText();
                    debugIn.put("inContent", inContent);
                    JsonNode rawMetadata = body.get("metadata");
                    ObjectNode metadataNode;

                    if (rawMetadata == null) {
                        metadataNode = JacksonUtil.newObjectNode();
                    } else if (rawMetadata.isObject()) {
                        metadataNode = (ObjectNode) rawMetadata;
                    } else {
                        metadataNode = JacksonUtil.fromString(rawMetadata.asText(), ObjectNode.class);
                    }

                    if (!metadataNode.has(INCLUDE_GATEWAY_INFO)) {
                        metadataNode.put(INCLUDE_GATEWAY_INFO, IS_GATEWAY_INFO_INCLUDED);
                    }

                    debugIn.put("inMetadata", metadataNode.toString());
                } else { //Downlink
                    String inContent = "";
                    String inMsgType = "";
                    String inMetadata = "";
                    String in = body.get("in").asText();
                    JsonNode inJson = JacksonUtil.toJsonNode(in);
                    if (inJson.isArray() && !inJson.isEmpty()) {
                        JsonNode msgJson = inJson.get(inJson.size() - 1);
                        JsonNode msg = msgJson.get("msg");
                        if (msg.isTextual()) {
                            inContent = "";
                        } else if (msg.isObject()) {
                            inContent = JacksonUtil.toString(msg);
                        }
                        inMsgType = msgJson.get("msgType").asText();
                        inMetadata = JacksonUtil.toString(msgJson.get("metadata"));
                    }
                    debugIn.put("inContent", inContent);
                    debugIn.put("inMsgType", inMsgType);
                    debugIn.put("inMetadata", inMetadata);
                    String inIntegrationMetadata = body.get("metadata").asText();
                    debugIn.put("inIntegrationMetadata", inIntegrationMetadata);
                }
            }
        }
        return debugIn;
    }

    @ApiOperation(value = "Test converter function (testUpLinkConverter)",
            notes = "Returns a JSON object representing the result of the processed incoming message. " + NEW_LINE + TEST_UPLINK_CONVERTER_DEFINITION)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/converter/testUpLink")
    public JsonNode testUpLinkConverter(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "A JSON value representing the input to the converter function.")
            @RequestBody JsonNode inputParams) {
        // Deliberately unguarded: this only evaluates the decoder against a sample payload and persists
        // nothing - it is the diagnostic surface of an integration that is still running.
        String payloadBase64 = inputParams.get("payload").asText();
        byte[] payload = Base64.getDecoder().decode(payloadBase64);
        JsonNode metadata = inputParams.get("metadata");
        String decoder = inputParams.get("decoder").asText();

        Map<String, Object> metadataMap = JacksonUtil.convertValue(metadata, new TypeReference<>() {});
        UplinkMetaData<Object> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, metadataMap);

        String output = "";
        String errorText = "";
        ScriptUplinkEvaluator scriptUplinkEvaluator = null;
        try {
            scriptUplinkEvaluator = new ScriptUplinkEvaluator(getTenantId(), getScriptInvokeService(scriptLang), getCurrentUser().getId(), decoder);
            output = scriptUplinkEvaluator.execute(payload, uplinkMetaData).get();
        } catch (Exception e) {
            log.error("Error evaluating JS UpLink Converter function", e);
            errorText = e.getMessage();
        } finally {
            if (scriptUplinkEvaluator != null) {
                scriptUplinkEvaluator.destroy();
            }
        }
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("output", output);
        result.put("error", errorText);

        if (StringUtils.isNotEmpty(output) && inputParams.has("converter")) {
            Converter converter = JacksonUtil.treeToValue(inputParams.get("converter"), Converter.class);
            if (converter.isDedicated() && ConverterUnwrapperFactory.getUnwrapper(converter.getIntegrationType()).isPresent()) {
                DedicatedConverterConfig config = JacksonUtil.treeToValue(converter.getConfiguration(), DedicatedConverterConfig.class);
                JsonElement jsonOutput = JsonParser.parseString(output);
                Object outputMsg = null;
                if (jsonOutput.isJsonArray()) {
                    List<DedicatedUplinkData> resultList = new ArrayList<>();
                    for (JsonElement uplinkJson : jsonOutput.getAsJsonArray()) {
                        resultList.add(parseUplinkData(config, uplinkJson.getAsJsonObject(), uplinkMetaData));
                    }
                    outputMsg = resultList;
                } else if (jsonOutput.isJsonObject()) {
                    outputMsg = parseUplinkData(config, jsonOutput.getAsJsonObject(), uplinkMetaData);
                }
                if (outputMsg != null) {
                    result.set("outputMsg", JacksonUtil.valueToTree(outputMsg));
                }
            }
        }

        return result;
    }

    @ApiOperation(value = "Test converter function (testDownLinkConverter)",
            notes = "Returns a JSON object representing the result of the processed incoming message. " + NEW_LINE +
                    TEST_DOWNLINK_CONVERTER_DEFINITION
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/converter/testDownLink")
    public JsonNode testDownLinkConverter(
            @Parameter(description = "Script language: JS or TBEL")
            @RequestParam(required = false) ScriptLanguage scriptLang,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "A JSON value representing the input to the converter function.")
            @RequestBody JsonNode inputParams) throws Exception {
        // Deliberately unguarded: this only evaluates the encoder against a sample message and persists nothing.
        String data = inputParams.get("msg").asText();
        JsonNode metadata = inputParams.get("metadata");
        String msgType = inputParams.get("msgType").asText();
        JsonNode integrationMetadata = inputParams.get("integrationMetadata");
        String encoder = inputParams.get("encoder").asText();

        Map<String, String> metadataMap = JacksonUtil.convertValue(metadata, new TypeReference<>() {});

        Map<String, String> integrationMetadataMap = JacksonUtil.convertValue(integrationMetadata, new TypeReference<>() {});
        IntegrationMetaData integrationMetaData = new IntegrationMetaData(integrationMetadataMap);

        JsonNode output = null;
        String errorText = "";
        ScriptDownlinkEvaluator scriptDownlinkEvaluator = null;
        try {
            TbMsg inMsg = TbMsg.newMsg()
                    .type(msgType)
                    .metaData(new TbMsgMetaData(metadataMap))
                    .data(data)
                    .build();
            scriptDownlinkEvaluator = new ScriptDownlinkEvaluator(getTenantId(), getScriptInvokeService(scriptLang), getCurrentUser().getId(), encoder);
            output = scriptDownlinkEvaluator.execute(inMsg, integrationMetaData);
            validateDownLinkOutput(output);
        } catch (Exception e) {
            log.error("Error evaluating JS Downlink Converter function", e);
            errorText = e.getMessage();
        } finally {
            if (scriptDownlinkEvaluator != null) {
                scriptDownlinkEvaluator.destroy();
            }
        }
        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("output", JacksonUtil.toString(output));
        result.put("error", errorText);
        return result;
    }

    private ScriptInvokeService getScriptInvokeService(ScriptLanguage scriptLang) {
        ScriptInvokeService scriptInvokeService;
        if (scriptLang == null) {
            scriptLang = ScriptLanguage.JS;
        }
        if (ScriptLanguage.JS.equals(scriptLang)) {
            scriptInvokeService = jsInvokeService;
        } else {
            if (tbelInvokeService.isEmpty()) {
                throw new IllegalArgumentException("TBEL script engine is disabled!");
            }
            scriptInvokeService = tbelInvokeService.get();
        }
        return scriptInvokeService;
    }

    private void validateDownLinkOutput(JsonNode output) throws Exception {
        if (output.isArray()) {
            for (JsonNode downlinkJson : output) {
                if (downlinkJson.isObject()) {
                    validateDownLinkObject(downlinkJson);
                } else {
                    throw new JsonParseException("Invalid downlink output format!");
                }
            }
        } else if (output.isObject()) {
            validateDownLinkObject(output);
        } else {
            throw new JsonParseException("Invalid downlink output format!");
        }
    }

    private void validateDownLinkObject(JsonNode src) throws Exception {
        AbstractDownlinkDataConverter.parseDownlinkData(src);
    }

    @Hidden
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/converters", params = {"converterIds"})
    public List<Converter> getConvertersByIdsV1(
            @RequestParam("converterIds") String[] strConverterIds) throws Exception {
        checkArrayParameter("converterIds", strConverterIds);
        if (!accessControlService.hasPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ)) {
            return Collections.emptyList();
        }
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        List<ConverterId> converterIds = new ArrayList<>();
        for (String strConverterId : strConverterIds) {
            converterIds.add(new ConverterId(toUUID(strConverterId)));
        }
        List<Converter> converters = checkNotNull(converterService.findConvertersByIdsAsync(tenantId, converterIds).get());
        return filterConvertersByReadPermission(converters);
    }

    @ApiOperation(value = "Get Converters By Ids (getConvertersByIds)",
            notes = "Requested converters must be owned by tenant which is performing the request. " +
                    NEW_LINE + RBAC_READ_CHECK)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(value = "/converters/list")
    public List<Converter> getConvertersByIds(
            @Parameter(description = "A list of converter ids, separated by comma ','", array = @ArraySchema(schema = @Schema(type = "string")), required = true)
            @RequestParam("converterIds") String[] strConverterIds) throws Exception {
        return getConvertersByIdsV1(strConverterIds);
    }

    @ApiOperation(value = "Transform input raw payload to the dedicated converter data (unwrapRawPayload)",
            notes = "Returns a JSON object representing the result of the unwrapped incoming raw message. " + NEW_LINE +
                    DEDICATED_CONVERTER_DEFINITION
    )
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(value = "/converter/unwrap/{integrationType}")
    public JsonNode unwrapRawPayload(@Parameter(description = INTEGRATION_TYPE_DESCRIPTION)
                                     @PathVariable IntegrationType integrationType,
                                     @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "A JSON value representing the input message.")
                                     @RequestBody JsonNode inputParams) throws Exception {
        // Deliberately unguarded: this only unwraps a sample raw payload and persists nothing.
        JsonNode payloadJson = inputParams.get("payload");
        byte[] payload = JacksonUtil.writeValueAsBytes(payloadJson);
        JsonNode metadata = inputParams.get("metadata");

        Map<String, Object> metadataMap = JacksonUtil.convertValue(metadata, new TypeReference<>() {});
        UplinkMetaData<Object> uplinkMetaData = new UplinkMetaData<>(ContentType.JSON, metadataMap);

        var unwrapper = ConverterUnwrapperFactory
                .getUnwrapper(integrationType)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported integrationType: " + integrationType));
        TbPair<byte[], UplinkMetaData<Object>> wrappedPair = unwrapper.unwrap(payload, uplinkMetaData);
        payload = wrappedPair.getFirst();
        uplinkMetaData = wrappedPair.getSecond();

        ObjectNode result = JacksonUtil.newObjectNode();
        result.put("contentType", uplinkMetaData.getContentType().toString());
        result.set("metadata", JacksonUtil.valueToTree(uplinkMetaData.getKvMap()));

        String payloadValue;
        if (uplinkMetaData.getContentType() == ContentType.BINARY) {
            payloadValue = Base64.getEncoder().encodeToString(payload);
        } else {
            payloadValue = new String(payload, StandardCharsets.UTF_8);
        }
        result.put("payload", payloadValue);

        return result;
    }

    private List<Converter> filterConvertersByReadPermission(List<Converter> converters) {
        return converters.stream().filter(converter -> {
            try {
                return accessControlService.hasPermission(getCurrentUser(), Resource.CONVERTER, Operation.READ, converter.getId(), converter);
            } catch (ThingsboardException e) {
                return false;
            }
        }).toList();
    }

}
