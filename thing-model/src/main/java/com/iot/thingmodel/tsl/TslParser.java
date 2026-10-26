package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.iot.thingmodel.common.Constants.Tsl.KEY_ACTIONS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_CATEGORY_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_DEFINE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_DESC;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_EVENTS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_EVENT_TYPE_FIELD;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_FIELDS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_INPUT;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_INPUT_PARAMS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MAPPING;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MAX;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MIN;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MODE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_NAME;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_OUTPUT;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_OUTPUT_PARAMS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PARAMS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PRODUCT_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PROFILE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PROFILE_CATEGORY_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PROFILE_PRODUCT_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PROPERTIES;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_REQUIRED;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_SPECS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_START;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_STEP;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_UNIT;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.MODE_DEFAULT;

@Component
public class TslParser {

    private final ObjectMapper objectMapper;

    public TslParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TslDefinition parse(JsonNode root) {
        if (root == null || root.isNull()) {
            return new TslDefinition(null, null, null, List.of(), List.of(), List.of(), root);
        }
        if (root.isTextual()) {
            try {
                root = objectMapper.readTree(root.asText());
            } catch (Exception ex) {
                return new TslDefinition(null, null, null, List.of(), List.of(), List.of(), root);
            }
        }
        JsonNode profile = root.path(KEY_PROFILE);
        String productId = text(profile, KEY_PROFILE_PRODUCT_ID, KEY_PRODUCT_ID);
        String categoryId = text(profile, KEY_PROFILE_CATEGORY_ID, KEY_CATEGORY_ID);
        String version = text(root, KEY_VERSION);

        List<TslProperty> properties = new ArrayList<>();
        for (JsonNode property : array(root, KEY_PROPERTIES)) {
            JsonNode defineNode = property.path(KEY_DEFINE);
            TslDefine define = parseDefine(defineNode);
            JsonNode mapping = firstObject(property.get(KEY_MAPPING), define.mapping());
            properties.add(new TslProperty(
                    text(property, KEY_ID),
                    text(property, KEY_NAME),
                    text(property, KEY_DESC),
                    defaultValue(text(property, KEY_MODE), MODE_DEFAULT),
                    property.path(KEY_REQUIRED).asBoolean(false),
                    define,
                    mapping));
        }

        List<TslEvent> events = new ArrayList<>();
        for (JsonNode event : array(root, KEY_EVENTS)) {
            events.add(new TslEvent(
                    text(event, KEY_ID),
                    text(event, KEY_NAME),
                    text(event, KEY_DESC),
                    text(event, KEY_TYPE, KEY_EVENT_TYPE_FIELD),
                    parseParams(firstArray(event, KEY_PARAMS, KEY_OUTPUT))));
        }

        List<TslAction> actions = new ArrayList<>();
        for (JsonNode action : array(root, KEY_ACTIONS)) {
            actions.add(new TslAction(
                    text(action, KEY_ID),
                    text(action, KEY_NAME),
                    text(action, KEY_DESC),
                    parseParams(firstArray(action, KEY_INPUT, KEY_INPUT_PARAMS)),
                    parseParams(firstArray(action, KEY_OUTPUT, KEY_OUTPUT_PARAMS))));
        }

        return new TslDefinition(version, productId, categoryId, properties, events, actions, root);
    }

    private List<TslParam> parseParams(JsonNode params) {
        List<TslParam> result = new ArrayList<>();
        if (params == null || !params.isArray()) {
            return result;
        }
        for (JsonNode param : params) {
            result.add(new TslParam(
                    text(param, KEY_ID),
                    text(param, KEY_NAME),
                    text(param, KEY_DESC),
                    parseDefine(param.path(KEY_DEFINE))));
        }
        return result;
    }

    private TslDefine parseDefine(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new TslDefine(null, null, null, null, null, null, null, null);
        }
        return new TslDefine(
                text(node, KEY_TYPE),
                number(node, KEY_MIN),
                number(node, KEY_MAX),
                number(node, KEY_START),
                number(node, KEY_STEP),
                text(node, KEY_UNIT),
                firstObject(node.get(KEY_SPECS), node.get(KEY_FIELDS)),
                node.get(KEY_MAPPING));
    }

    private List<JsonNode> array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        value.forEach(result::add);
        return result;
    }

    private JsonNode firstArray(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    private JsonNode firstObject(JsonNode first, JsonNode second) {
        if (first != null && !first.isNull()) {
            return first;
        }
        return second;
    }

    private String text(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asDouble();
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String defaultValue(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
