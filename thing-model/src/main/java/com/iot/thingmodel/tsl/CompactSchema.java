package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_EVENTS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_MAP;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_REQUIRED;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_TSL_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_COMPACT_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_KEYS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MAX;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MIN;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_MODE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PARAMS;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_PRODUCT_ID;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_SCHEMA_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_START;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_STEP;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_STRICT_UNKNOWN;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_TYPE;
import static com.iot.thingmodel.common.Constants.Tsl.KEY_UNIT;
import static com.iot.thingmodel.common.Constants.Tsl.MODE_DEFAULT;
import static com.iot.thingmodel.common.Constants.Tsl.SCHEMA_VERSION;
import static com.iot.thingmodel.common.Constants.Tsl.TYPE_UNKNOWN;

@Component
public class CompactSchema {

    private final ObjectMapper objectMapper;

    public CompactSchema(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompactResult generate(TslDefinition definition, boolean strictUnknown) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
        root.put(KEY_PRODUCT_ID, definition.productId());
        putText(root, KEY_COMPACT_TSL_VERSION, definition.version());
        root.put(KEY_STRICT_UNKNOWN, strictUnknown);

        ObjectNode keys = root.putObject(KEY_KEYS);
        List<TslProperty> sorted = definition.properties().stream()
                .filter(property -> property.id() != null)
                .sorted(Comparator.comparing(TslProperty::id))
                .toList();
        for (TslProperty property : sorted) {
            ObjectNode item = keys.putObject(property.id());
            putDefine(item, property.define());
            item.put(KEY_MODE, property.mode() == null ? MODE_DEFAULT : property.mode());
            if (property.mapping() != null
                    && !property.mapping().isNull()
                    && item.get(KEY_COMPACT_MAP) == null) {
                item.set(KEY_COMPACT_MAP, property.mapping());
            }
            if (property.required()) {
                item.put(KEY_COMPACT_REQUIRED, true);
            }
        }

        ObjectNode events = root.putObject(KEY_COMPACT_EVENTS);
        List<TslEvent> sortedEvents = definition.events().stream()
                .filter(event -> event.id() != null)
                .sorted(Comparator.comparing(TslEvent::id))
                .toList();
        for (TslEvent event : sortedEvents) {
            ObjectNode item = events.putObject(event.id());
            putText(item, KEY_TYPE, event.type());
            ObjectNode params = item.putObject(KEY_PARAMS);
            event.params().stream()
                    .filter(param -> param.id() != null)
                    .sorted(Comparator.comparing(TslParam::id))
                    .forEach(param -> putDefine(params.putObject(param.id()), param.define()));
        }

        try {
            String json = objectMapper.writeValueAsString(root);
            return new CompactResult(json, json.getBytes(StandardCharsets.UTF_8).length, root);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize compact schema", ex);
        }
    }

    private void putDefine(ObjectNode item, TslDefine define) {
        item.put(
                KEY_COMPACT_TYPE,
                define == null || define.type() == null ? TYPE_UNKNOWN : define.type());
        if (define == null) {
            return;
        }
        putNumber(item, KEY_MIN, define.min());
        putNumber(item, KEY_MAX, define.max());
        putNumber(item, KEY_START, define.start());
        putNumber(item, KEY_STEP, define.step());
        putText(item, KEY_UNIT, define.unit());
        if (define.mapping() != null && !define.mapping().isNull()) {
            item.set(KEY_COMPACT_MAP, define.mapping());
        }
    }

    private void putNumber(ObjectNode node, String field, Double value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    private void putText(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value);
        }
    }

    public record CompactResult(String json, int bytes, JsonNode node) {
    }
}
