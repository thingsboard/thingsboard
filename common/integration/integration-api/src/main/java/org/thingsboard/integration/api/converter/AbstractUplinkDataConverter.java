// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapper;
import org.thingsboard.integration.api.converter.wrapper.ConverterUnwrapperFactory;
import org.thingsboard.integration.api.data.ContentType;
import org.thingsboard.integration.api.data.UplinkData;
import org.thingsboard.integration.api.data.UplinkMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public abstract class AbstractUplinkDataConverter extends AbstractDataConverter implements TBUplinkDataConverter {

    private static final String DEFAULT_DEVICE_TYPE = "default";

    private static final int MAX_ALLOWED_STRING_LENGTH = 32;

    protected final Set<String> updateOnlyKeys = new HashSet<>();
    protected final Map<String, Map<String, String>> currentUpdateOnlyTelemetryPerEntity = new ConcurrentHashMap<>();
    protected final Map<String, Map<String, String>> currentUpdateOnlyAttributesPerEntity = new ConcurrentHashMap<>();

    private ConverterUnwrapper converterUnwrapper;

    public AbstractUplinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
    }

    @Override
    public void init(Converter configuration) {
        this.configuration = configuration;
        JsonNode configurationNode = configuration.getConfiguration();

        this.updateOnlyKeys.clear();
        JsonNode updateOnlyKeysNode = configurationNode.get("updateOnlyKeys");

        if (updateOnlyKeysNode != null && updateOnlyKeysNode.isArray()) {
            updateOnlyKeysNode.elements().forEachRemaining(key -> this.updateOnlyKeys.add(getAllowedValue(key.asText())));
        }

        this.currentUpdateOnlyTelemetryPerEntity.values().forEach(entityKeys -> entityKeys.keySet().retainAll(this.updateOnlyKeys));
        this.currentUpdateOnlyAttributesPerEntity.values().forEach(entityKeys -> entityKeys.keySet().retainAll(this.updateOnlyKeys));

        if (configuration.isDedicated()) {
            var integrationType = configuration.getIntegrationType();
            this.converterUnwrapper = ConverterUnwrapperFactory
                    .getUnwrapper(integrationType)
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported integrationType: " + integrationType));
        }
    }

    @Override
    public ListenableFuture<List<UplinkData>> convertUplink(ConverterContext context, byte[] data, UplinkMetaData metadata, ExecutorService callBackExecutorService) throws Exception {
        final byte[] srcData = data;
        final UplinkMetaData srcMetadata = metadata;
        final byte[] finalData;
        final UplinkMetaData finalMetadata;

        if (configuration.isDedicated()) {
            TbPair<byte[], UplinkMetaData<Object>> wrappedPair = converterUnwrapper.unwrap(data, metadata);
            finalData = wrappedPair.getFirst();
            finalMetadata = wrappedPair.getSecond();
        } else {
            finalData = data;
            finalMetadata = metadata;
        }

        long startTime = System.currentTimeMillis();
        ListenableFuture<String> convertFuture = doConvertUplink(finalData, finalMetadata);
        ListenableFuture<List<UplinkData>> result = Futures.transform(convertFuture, rawResult -> {
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Uplink conversion took {} ms.", configuration.getId(), configuration.getName(), System.currentTimeMillis() - startTime);
            }
            JsonElement element = JsonParser.parseString(rawResult);
            List<UplinkData> resultList = new ArrayList<>();
            if (element.isJsonArray()) {
                for (JsonElement uplinkJson : element.getAsJsonArray()) {
                    resultList.add(parseUplinkData(uplinkJson.getAsJsonObject(), finalMetadata));
                }
            } else if (element.isJsonObject()) {
                resultList.add(parseUplinkData(element.getAsJsonObject(), finalMetadata));
            }
            if (DebugModeUtil.isDebugAllAvailable(configuration)) {
                if (context.getRateLimitService().map(s -> s.checkLimit(configuration.getTenantId(), configuration.getId(), false)).orElse(true)) {
                    persistUplinkDebug(context, srcMetadata.getContentType(), srcData, rawResult, srcMetadata);
                } else {
                    if (context.getRateLimitService().get().alreadyProcessed(configuration.getId(), EntityType.CONVERTER)) {
                        log.trace("[{}] [{}] [{}] Rate limited debug event already sent.", configuration.getTenantId(), configuration.getId(), EntityType.CONVERTER);
                    } else {
                        TbRateLimitsException exception = new TbRateLimitsException(EntityType.CONVERTER, "Converter debug rate limits reached!");
                        persistUplinkDebug(context, srcMetadata.getContentType(), srcData, srcMetadata, exception);
                    }
                }
            }
            return resultList;
        }, callBackExecutorService);
        DonAsynchron.withCallback(result, r -> {
        }, t -> {
            if (t instanceof Exception) {
                if (DebugModeUtil.isDebugIntegrationFailuresAvailable(configuration)) {
                    persistUplinkDebug(context, srcMetadata.getContentType(), srcData, srcMetadata, (Exception) t);
                }
            } else {
                log.warn("[{}][{}] Unhandled exception: ", configuration.getId(), configuration.getName(), t);
            }
        }, callBackExecutorService);
        return result;
    }

    protected abstract ListenableFuture<String> doConvertUplink(byte[] data, UplinkMetaData metadata) throws Exception;

    protected UplinkData parseUplinkData(JsonObject src, UplinkMetaData metadata) {
        boolean isAsset = getIsAssetAndVerify(src);

        UplinkData.UplinkDataBuilder builder = UplinkData.builder();
        builder.isAsset(isAsset);
        String entityName;
        if (isAsset) {
            entityName = src.get("assetName").getAsString();
            builder.assetName(entityName);
            builder.assetType(src.get("assetType").getAsString());
            if (src.has("assetLabel")) {
                builder.assetLabel(src.get("assetLabel").getAsString());
            }
        } else {
            entityName = src.get("deviceName").getAsString();
            builder.deviceName(entityName);
            if (src.has("deviceType")) {
                builder.deviceType(src.get("deviceType").getAsString());
            } else {
                builder.deviceType(DEFAULT_DEVICE_TYPE);
            }
            if (src.has("deviceLabel")) {
                builder.deviceLabel(src.get("deviceLabel").getAsString());
            }
        }

        if (src.has("customerName")) {
            builder.customerName(src.get("customerName").getAsString());
        }
        if (src.has("groupName")) {
            builder.groupName(src.get("groupName").getAsString());
        }

        Map<String, String> currentOnValueTelemetryUpdate = this.currentUpdateOnlyTelemetryPerEntity.getOrDefault(entityName, new ConcurrentHashMap<>());
        Map<String, String> currentOnValueAttributesUpdate = this.currentUpdateOnlyAttributesPerEntity.getOrDefault(entityName, new ConcurrentHashMap<>());
        if (src.has("telemetry")) {
            PostTelemetryMsg parsedTelemetry = parseTelemetry(src.get("telemetry"));
            if (!this.updateOnlyKeys.isEmpty()) {
                parsedTelemetry = filterTelemetryOnKeyValueUpdateAndUpdateMap(parsedTelemetry, currentOnValueTelemetryUpdate);
            }
            builder.telemetry(parsedTelemetry);
        }
        if (src.has("attributes")) {
            PostAttributeMsg attributes = parseAttributesUpdate(src.get("attributes"));
            if (!this.updateOnlyKeys.isEmpty()) {
                attributes = filterAttributeOnKeyValueUpdateAndUpdateMap(attributes, currentOnValueAttributesUpdate);
            }
            builder.attributesUpdate(attributes);
        }

        if (!currentOnValueTelemetryUpdate.isEmpty()) {
            this.currentUpdateOnlyTelemetryPerEntity.put(entityName, currentOnValueTelemetryUpdate);
        }

        if (!currentOnValueAttributesUpdate.isEmpty()) {
            this.currentUpdateOnlyAttributesPerEntity.put(entityName, currentOnValueAttributesUpdate);
        }

        //TODO: add support of attribute requests and client-side RPC.
        return builder.build();
    }

    protected PostTelemetryMsg filterTelemetryOnKeyValueUpdateAndUpdateMap(PostTelemetryMsg telemetry, Map<String, String> currentEntityKeyValues) {
        PostTelemetryMsg.Builder filteredTelemetryBuilder = PostTelemetryMsg.newBuilder();
        for (TransportProtos.TsKvListProto tsKvList : telemetry.getTsKvListList()) {
            TransportProtos.TsKvListProto.Builder filteredTsKvListBuilder = TransportProtos.TsKvListProto.newBuilder().setTs(tsKvList.getTs());

            List<TransportProtos.KeyValueProto> filtered = filterKeyValueAndUpdateMap(tsKvList.getKvList(), currentEntityKeyValues);
            filteredTsKvListBuilder.addAllKv(filtered);

            if (!filteredTsKvListBuilder.getKvList().isEmpty()) {
                filteredTelemetryBuilder.addTsKvList(filteredTsKvListBuilder.build());
            }
        }

        return !currentEntityKeyValues.isEmpty() ? filteredTelemetryBuilder.build() : telemetry;
    }

    protected PostAttributeMsg filterAttributeOnKeyValueUpdateAndUpdateMap(PostAttributeMsg attributes, Map<String, String> currentEntityKeyValues) {
        PostAttributeMsg.Builder filteredAttributesBuilder = PostAttributeMsg.newBuilder();
        List<TransportProtos.KeyValueProto> filtered = filterKeyValueAndUpdateMap(attributes.getKvList(), currentEntityKeyValues);
        filteredAttributesBuilder.addAllKv(filtered);
        return !currentEntityKeyValues.isEmpty() ? filteredAttributesBuilder.build() : attributes;
    }

    List<TransportProtos.KeyValueProto> filterKeyValueAndUpdateMap(List<TransportProtos.KeyValueProto> kvList, Map<String, String> currentEntityKeyValues) {
        List<TransportProtos.KeyValueProto> filtered = new ArrayList<>();
        for (TransportProtos.KeyValueProto keyValue : kvList) {
            String key = getAllowedValue(keyValue.getKey());
            boolean isOnValueUpdate = this.updateOnlyKeys.contains(key);
            if (isOnValueUpdate) {
                String value = getValueAsAllowedString(keyValue);
                boolean shouldAddToResult = currentEntityKeyValues.isEmpty() ||
                        !currentEntityKeyValues.containsKey(key) ||
                        !currentEntityKeyValues.get(key).equals(value);
                if (shouldAddToResult) {
                    filtered.add(keyValue);
                    currentEntityKeyValues.put(key, value);
                }
            } else {
                filtered.add(keyValue);
            }
        }
        return filtered;
    }

    private String getValueAsAllowedString(TransportProtos.KeyValueProto keyValueProto) {
        switch (keyValueProto.getType()) {
            case STRING_V:
                return getAllowedValue(keyValueProto.getStringV());
            case JSON_V:
                return getAllowedValue(keyValueProto.getJsonV());
            case DOUBLE_V:
                return getAllowedValue(String.valueOf(keyValueProto.getDoubleV()));
            case LONG_V:
                return getAllowedValue(String.valueOf(keyValueProto.getLongV()));
            case BOOLEAN_V:
                return getAllowedValue(String.valueOf(keyValueProto.getBoolV()));
            default:
                return null;
        }
    }

    private boolean getIsAssetAndVerify(JsonObject src) {
        boolean isAsset;
        boolean isDeviceNamePresent = src.has("deviceName");
        boolean isAssetNamePresent = src.has("assetName");
        boolean isAssetTypePresent = src.has("assetType");

        if (!isDeviceNamePresent && !isAssetNamePresent) {
            throw new JsonParseException("Either 'deviceName' or 'assetName' should be present in the converter output!");
        }
        if (isDeviceNamePresent && isAssetNamePresent) {
            throw new JsonParseException("Both 'deviceName' and 'assetName' can't be present in the converter output!");
        }

        if (isDeviceNamePresent) {
            isAsset = false;
        } else {
            if (!isAssetTypePresent) {
                throw new JsonParseException("Asset type is not set!");
            }
            isAsset = true;
        }
        return isAsset;
    }

    protected PostTelemetryMsg parseTelemetry(JsonElement src) {
        return JsonConverter.convertToTelemetryProto(src);
    }

    protected PostAttributeMsg parseAttributesUpdate(JsonElement src) {
        return JsonConverter.convertToAttributesProto(src);
    }

    private void persistUplinkDebug(ConverterContext context, ContentType inMessageType, byte[] inMessage, String outMessage, UplinkMetaData metadata) {
        try {
            persistDebug(context, getTypeUplink(inMessage), inMessageType.name(), inMessage, "JSON", outMessage.getBytes(StandardCharsets.UTF_8), metadataToJson(metadata), null);
        } catch (JsonProcessingException e) {
            log.warn("Failed to persist uplink debug message");
        }
    }

    private void persistUplinkDebug(ConverterContext context, ContentType inMessageType, byte[] inMessage, UplinkMetaData metadata, Exception e) {
        try {
            persistDebug(context, "Uplink", inMessageType.name(), inMessage, null, null, metadataToJson(metadata), e);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to persist uplink debug message", ex);
        }
    }

    private String metadataToJson(UplinkMetaData metaData) throws JsonProcessingException {
        return JacksonUtil.toString(new TreeMap<>(metaData.getKvMap()));
    }

    private String getTypeUplink(byte[] inMessage) throws JsonProcessingException {
        return (inMessage != null && inMessage.length > 23 && Arrays.equals(Arrays.copyOfRange(inMessage, 1, 23), JacksonUtil.writeValueAsBytes("DevEUI_downlink_Sent"))) ? "Downlink_Sent" : "Uplink";
    }

    private String getAllowedValue(String incoming) {
        return incoming.length() > MAX_ALLOWED_STRING_LENGTH ? DigestUtils.sha1Hex(incoming) : incoming;
    }
}
