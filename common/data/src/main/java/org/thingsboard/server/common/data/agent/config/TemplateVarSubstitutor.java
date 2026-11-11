// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${var.<name>}} and {@code ${var.!<name>}} (boolean negation) placeholders against a map of
 * materialization variables, operating on a raw {@link JsonNode} tree BEFORE it is deserialized into the typed
 * template model. This matters because an abstract template stores placeholders as strings in positions that the
 * typed model expects to be concrete (e.g. a boolean {@code pullImages.value}); substituting at the JSON level lets a
 * whole-string token be replaced with a typed node (boolean/number), while an embedded token (e.g. inside a compose
 * path) is replaced textually.
 *
 * <p>Only the {@code ${var.*}} family is handled here; {@code ${compose.*}} references are intentionally left intact
 * for runtime resolution (see {@link AgentComposeRefUtils}). Unknown variables are left untouched (fail-open).
 */
@Slf4j
public class TemplateVarSubstitutor {

    // Matches a single ${var.name} or ${var.!name} token anywhere in a string.
    private static final Pattern VAR_TOKEN = Pattern.compile("\\$\\{var\\.(!?)([A-Za-z0-9_]+)}");
    // Matches a string that is EXACTLY one token (used to emit a typed node instead of text).
    private static final Pattern VAR_TOKEN_FULL = Pattern.compile("^\\$\\{var\\.(!?)([A-Za-z0-9_]+)}$");

    private TemplateVarSubstitutor() {
    }

    /**
     * Returns a deep copy of {@code template} with all {@code ${var.*}} placeholders resolved against {@code vars}.
     * The input node is not mutated.
     */
    public static JsonNode substitute(JsonNode template, Map<String, Object> vars) {
        if (template == null) {
            return null;
        }
        return visit(template.deepCopy(), vars == null ? Map.of() : vars);
    }

    private static JsonNode visit(JsonNode node, Map<String, Object> vars) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String field : fieldNames) {
                obj.set(field, visit(obj.get(field), vars));
            }
            return obj;
        }
        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, visit(arr.get(i), vars));
            }
            return arr;
        }
        if (node.isTextual()) {
            return substituteText(node.textValue(), vars);
        }
        return node;
    }

    private static JsonNode substituteText(String text, Map<String, Object> vars) {
        // Whole-string token -> emit a typed node (so boolean/number targets deserialize correctly).
        Matcher full = VAR_TOKEN_FULL.matcher(text);
        if (full.matches()) {
            boolean negate = "!".equals(full.group(1));
            String name = full.group(2);
            if (!vars.containsKey(name)) {
                log.warn("Template variable [{}] not found; leaving placeholder untouched", name);
                return new TextNode(text);
            }
            Object value = vars.get(name);
            if (negate) {
                return BooleanNode.valueOf(!truthy(value));
            }
            return toNode(value);
        }

        // Embedded token(s) -> textual replacement.
        Matcher m = VAR_TOKEN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            boolean negate = "!".equals(m.group(1));
            String name = m.group(2);
            String replacement;
            if (!vars.containsKey(name)) {
                log.warn("Template variable [{}] not found; leaving placeholder untouched", name);
                replacement = Matcher.quoteReplacement(m.group(0));
            } else {
                Object value = vars.get(name);
                replacement = Matcher.quoteReplacement(negate ? String.valueOf(!truthy(value)) : String.valueOf(value));
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return new TextNode(sb.toString());
    }

    private static JsonNode toNode(Object value) {
        if (value == null) {
            return NullNode.getInstance();
        }
        if (value instanceof Boolean b) {
            return BooleanNode.valueOf(b);
        }
        if (value instanceof Integer || value instanceof Long) {
            return new LongNode(((Number) value).longValue());
        }
        if (value instanceof Number n) {
            return new DoubleNode(n.doubleValue());
        }
        return new TextNode(value.toString());
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
