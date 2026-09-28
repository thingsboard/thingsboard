// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.integration.api.data.DownlinkData;
import org.thingsboard.integration.api.data.IntegrationMetaData;
import org.thingsboard.integration.api.util.LogSettingsComponent;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractDownlinkDataConverter extends AbstractDataConverter implements TBDownlinkDataConverter {

    public AbstractDownlinkDataConverter(JsInvokeService jsInvokeService, TbelInvokeService tbelInvokeService, LogSettingsComponent logSettings) {
        super(jsInvokeService, tbelInvokeService, logSettings);
    }

    @Override
    public List<DownlinkData> convertDownLink(ConverterContext context, List<TbMsg> downLinkMsgs, IntegrationMetaData metadata) throws Exception {
        try {
            List<DownlinkData> result = new ArrayList<>();
            List<JsonNode> rawResults = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            for (TbMsg downLinkMsg : downLinkMsgs) {
                JsonNode rawResult = doConvertDownlink(downLinkMsg, metadata);
                rawResults.add(rawResult);
                if (rawResult.isArray()) {
                    for (JsonNode downlinkJson : rawResult) {
                        result.add(parseDownlinkData(downlinkJson));
                    }
                } else if (rawResult.isObject()) {
                    result.add(parseDownlinkData(rawResult));
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("[{}][{}] Downlink conversion took {} ms.", configuration.getId(), configuration.getName(), System.currentTimeMillis() - startTime);
            }
            if (DebugModeUtil.isDebugAllAvailable(configuration)) {
                persistDownlinkDebug(context, downLinkMsgs, rawResults, metadata);
            }
            return result;
        } catch (Exception e) {
            if (DebugModeUtil.isDebugIntegrationFailuresAvailable(configuration)) {
                persistDownlinkDebug(context, downLinkMsgs, metadata, e);
            }
            throw e;
        }
    }

    protected abstract JsonNode doConvertDownlink(TbMsg msg, IntegrationMetaData metadata) throws Exception;

    public static DownlinkData parseDownlinkData(JsonNode src) {
        if (!(src.isObject())) {
            throw new JsonParseException("Invalid Downlink json type: " + src.getNodeType());
        }
        if (!src.has("contentType")) {
            throw new JsonParseException("Downlink content type is not set!");
        } else if (!src.has("data")) {
            throw new JsonParseException("Downlink data is not set!");
        }
        String contentType = src.get("contentType").asText();
        String strData = src.get("data").asText();
        byte[] data;
        switch (contentType) {
            case "JSON":
            case "TEXT":
                data = strData.getBytes(StandardCharsets.UTF_8);
                break;
            case "BINARY":
                data = Base64.getDecoder().decode(strData);
                break;
            default:
                throw new JsonParseException("Unknown downlink content type " + contentType);
        }
        Map<String, String> metadata = new HashMap<>();
        if (src.has("metadata")) {
            JsonNode metadataElement = src.get("metadata");
            if (!metadataElement.isObject()) {
                throw new JsonParseException("Invalid downlink metadata format!");
            }
            metadataElement.properties().forEach((entry) -> {
                JsonNode metadataValue = entry.getValue();
                if (!metadataValue.isValueNode()) {
                    throw new JsonParseException("Invalid downlink metadata value format!");
                }
                metadata.put(entry.getKey(), metadataValue.asText());
            });
        }
        DownlinkData.DownlinkDataBuilder builder = DownlinkData.builder();
        builder.contentType(contentType);
        builder.data(data);
        builder.metadata(metadata);
        return builder.build();
    }

    private void persistDownlinkDebug(ConverterContext context, List<TbMsg> messages,
                                      List<JsonNode> rawResults, IntegrationMetaData metadata) {
        try {
            persistDebug(context, "Downlink", "JSON", msgListToJsonBytes(messages), "JSON", jsonListToJson(rawResults), metadataToJson(metadata), null);
        } catch (IOException e) {
            log.warn("Failed to persist downlink debug message");
        }
    }

    private void persistDownlinkDebug(ConverterContext context, List<TbMsg> messages, IntegrationMetaData metadata, Exception e) {
        try {
            persistDebug(context, "Downlink", "JSON", msgListToJsonBytes(messages), null, null, metadataToJson(metadata), e);
        } catch (IOException ex) {
            log.warn("Failed to persist downlink debug message", ex);
        }
    }

    private byte[] msgListToJsonBytes(List<TbMsg> messages) throws IOException {
        ArrayNode jsonArray = JacksonUtil.newArrayNode();
        for (TbMsg message : messages) {
            ObjectNode msgJson = JacksonUtil.newObjectNode();
            if (!StringUtils.isEmpty(message.getData())) {
                msgJson.set("msg", JacksonUtil.toJsonNode(message.getData()));
            } else {
                msgJson.put("msg", "");
            }
            msgJson.set("metadata", JacksonUtil.valueToTree(message.getMetaData().getData()));
            msgJson.put("msgType", message.getType());
            jsonArray.add(msgJson);
        }
        return JacksonUtil.toString(jsonArray).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] jsonListToJson(List<JsonNode> jsons) throws IOException {
        ArrayNode jsonArray = JacksonUtil.newArrayNode();
        for (JsonNode json : jsons) {
            jsonArray.add(json);
        }
        return JacksonUtil.toString(jsonArray).getBytes(StandardCharsets.UTF_8);
    }

    private String metadataToJson(IntegrationMetaData metaData) throws JsonProcessingException {
        return JacksonUtil.toString(metaData.getKvMap());
    }
}
