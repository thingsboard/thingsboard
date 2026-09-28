// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${compose.<path>}} references against a docker compose node. The dotted path navigates node fields
 * (e.g. {@code compose.version}); a {@code svcImgRegex(<image-regex>)} segment selects the first service whose image
 * matches the regex (from the current node's {@code services}), so a service can be picked without knowing its key.
 * Segment names are arbitrary — no whitelist. Shared across steps that copy values from the compose.
 */
@Slf4j
public class AgentComposeRefUtils {

    private static final Pattern COMPOSE_REF = Pattern.compile("^\\$\\{compose\\.(.+)}$");
    private static final Pattern SVC_IMG_REGEX = Pattern.compile("svcImgRegex\\((.+)\\)");

    private AgentComposeRefUtils() {
    }

    public static boolean isComposeRef(String value) {
        return value != null && COMPOSE_REF.matcher(value).matches();
    }

    /**
     * Navigates the {@code ${compose.<path>}} reference from {@code compose} and returns the node it points at, or
     * {@code null} when the ref is not a compose ref or any path segment is absent.
     */
    public static JsonNode resolve(JsonNode compose, String ref) {
        if (compose == null || ref == null) {
            return null;
        }
        Matcher m = COMPOSE_REF.matcher(ref);
        if (!m.matches()) {
            return null;
        }
        JsonNode node = compose;
        for (String segment : splitPath(m.group(1))) {
            if (node == null) {
                break;
            }
            Matcher svc = SVC_IMG_REGEX.matcher(segment);
            node = svc.matches()
                    ? DockerComposeUtils.findServiceByImage(node, Pattern.compile(svc.group(1)))
                    : node.get(segment);
        }
        if (node == null) {
            log.warn("Compose ref [{}] did not resolve to any node", ref);
        }
        return node;
    }

    /**
     * Resolves a ref for a list-typed target, normalizing the node to strings: an array yields its elements
     * (long-syntax objects such as {@code {type: bind, source: ., target: /app}} are collapsed to
     * {@code source:target[:ro]}); an object (compose map form, e.g. environment) yields {@code KEY=VALUE}
     * entries, with a null value yielding {@code KEY=}; a scalar yields a single element. An unresolved ref
     * yields an empty list (fail-open).
     */
    public static List<String> resolveAsList(JsonNode compose, String ref) {
        JsonNode node = resolve(compose, ref);
        List<String> out = new ArrayList<>();
        if (node == null || node.isNull()) {
            return out;
        }
        if (node.isArray()) {
            node.forEach(e -> {
                String value = elementAsString(e);
                if (value != null) {
                    out.add(value);
                }
            });
        } else if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                JsonNode value = e.getValue();
                out.add(e.getKey() + "=" + (value == null || value.isNull() ? "" : value.asText()));
            }
        } else {
            out.add(node.asText());
        }
        return out;
    }

    /**
     * @return the element rendered as a string, or {@code null} when it carries nothing usable — never the
     * empty string {@code asText()} returns for a container node, which would reach the agent as a bogus entry.
     */
    private static String elementAsString(JsonNode element) {
        if (element == null || element.isNull()) {
            return null;
        }
        if (element.isValueNode()) {
            return element.asText();
        }
        if (element.isObject()) {
            return longSyntaxMountAsString(element);
        }
        log.warn("Compose list element of type {} cannot be normalized to a string, skipping", element.getNodeType());
        return null;
    }

    /**
     * Collapses the long syntax of a volume/mount entry to the short {@code source:target[:ro]} form.
     */
    private static String longSyntaxMountAsString(JsonNode element) {
        String target = textOrNull(element, "target");
        if (target == null) {
            log.warn("Compose long-syntax mount entry has no 'target', skipping");
            return null;
        }
        String source = textOrNull(element, "source");
        StringBuilder sb = new StringBuilder();
        if (source != null) {
            sb.append(source).append(':');
        }
        sb.append(target);
        JsonNode readOnly = element.get("read_only");
        if (readOnly != null && readOnly.asBoolean(false)) {
            sb.append(":ro");
        }
        return sb.toString();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isValueNode() && !value.isNull() ? value.asText() : null;
    }

    // Splits on '.' only at parenthesis-depth 0, so dots inside svcImgRegex(<regex>) stay intact.
    private static List<String> splitPath(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == '.' && depth == 0) {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        segments.add(current.toString());
        return segments;
    }

}
