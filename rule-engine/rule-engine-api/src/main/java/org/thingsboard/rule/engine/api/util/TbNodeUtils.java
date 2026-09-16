// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TbNodeUtils {

    private TbNodeUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final Pattern DATA_PATTERN = Pattern.compile("(\\$\\[)(.*?)(])");

    private static final String ALL_DATA_TEMPLATE = "$[*]";
    private static final String ALL_METADATA_TEMPLATE = "${*}";

    public static <T> T convert(TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return JacksonUtil.treeToValue(configuration.getData(), clazz);
        } catch (IllegalArgumentException e) {
            throw new TbNodeException(e, true);
        }
    }

    public static List<String> processPatterns(List<String> patterns, TbMsg tbMsg) {
        if (CollectionsUtil.isEmpty(patterns)) {
            return Collections.emptyList();
        }
        return patterns.stream().map(p -> processPattern(p, tbMsg)).toList();
    }

    public static String processPattern(String pattern, TbMsg tbMsg) {
        return processPattern(pattern, tbMsg, false);
    }

    public static String processPattern(String pattern, TbMsg tbMsg, boolean escapeJsonValues) {
        try {
            String result = processPattern(pattern, tbMsg.getMetaData(), escapeJsonValues);
            JsonNode json = JacksonUtil.toJsonNode(tbMsg.getData());

            result = result.replace(ALL_DATA_TEMPLATE, JacksonUtil.toString(json));

            if (json.isObject()) {
                Matcher matcher = DATA_PATTERN.matcher(result);
                while (matcher.find()) {
                    String group = matcher.group(2);
                    String[] keys = group.split("\\.");
                    JsonNode jsonNode = json;
                    for (String key : keys) {
                        if (StringUtils.isNotEmpty(key) && jsonNode != null) {
                            jsonNode = jsonNode.get(key);
                        } else {
                            jsonNode = null;
                            break;
                        }
                    }

                    if (jsonNode != null && jsonNode.isValueNode()) {
                        String value = jsonNode.asText();
                        if (escapeJsonValues) {
                            value = escapeJsonValue(value);
                        }
                        result = result.replace(formatDataVarTemplate(group), value);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process pattern!", e);
        }
    }

    private static String processPattern(String pattern, TbMsgMetaData metaData, boolean escapeJsonValues) {
        String replacement = metaData.isEmpty() ? "{}" : JacksonUtil.toString(metaData.getData());
        pattern = pattern.replace(ALL_METADATA_TEMPLATE, replacement);
        return processTemplate(pattern, metaData.values(), escapeJsonValues);
    }

    private static String processPattern(String pattern, TbMsgMetaData metaData) {
        return processPattern(pattern, metaData, false);
    }

    public static String processTemplate(String template, Map<String, String> data) {
        return processTemplate(template, data, false);
    }

    private static String processTemplate(String template, Map<String, String> data, boolean escapeJsonValues) {
        String result = template;
        for (Map.Entry<String, String> kv : data.entrySet()) {
            String value = escapeJsonValues ? escapeJsonValue(kv.getValue()) : kv.getValue();
            result = result.replace(formatMetadataVarTemplate(kv.getKey()), value);
        }
        return result;
    }

    private static String escapeJsonValue(String value) {
        return new String(JsonStringEncoder.getInstance().quoteAsString(value));
    }

    static String formatDataVarTemplate(String key) {
        return "$[" + key + "]";
    }

    static String formatMetadataVarTemplate(String key) {
        return "${" + key + "}";
    }

}
