// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DockerComposeUtils {

    /**
     * Finds the environment node of the first service whose image matches the given pattern.
     * The returned node may be an {@link ObjectNode} (map form) or {@link ArrayNode} (list form).
     *
     * @return the environment node, or {@code null} if no matching service has an env block
     */
    public static JsonNode findServiceEnvironment(JsonNode compose, Pattern imagePattern) {
        JsonNode services = getServices(compose, imagePattern);
        if (services == null) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            if (!service.isObject() || !service.has("image")) {
                continue;
            }
            String image = service.get("image").asText();
            if (!imagePattern.matcher(image).matches()) {
                continue;
            }
            JsonNode environment = service.get("environment");
            if (environment != null && (environment.isObject() || environment.isArray())) {
                return environment;
            }
        }
        return null;
    }

    /**
     * Reads a single environment variable value from the first matching service.
     *
     * @return the variable value, or {@code null} if the service or variable is not found
     */
    public static String getEnvVariable(JsonNode compose, Pattern imagePattern, String envVarName) {
        return envGet(findServiceEnvironment(compose, imagePattern), envVarName);
    }

    /**
     * Reads the {@code image} of the first service whose image matches the given pattern.
     *
     * @return the image string, or {@code null} if no matching service is found
     */
    public static String getMainImage(JsonNode compose, Pattern imagePattern) {
        JsonNode service = findServiceByImage(compose, imagePattern);
        if (service == null) {
            return null;
        }
        JsonNode image = service.get("image");
        return image != null ? image.asText() : null;
    }

    /**
     * Sets the {@code image} on the first service whose current image matches the given pattern.
     */
    public static void setMainImage(JsonNode compose, Pattern imagePattern, String image) {
        JsonNode service = findServiceByImage(compose, imagePattern);
        if (service instanceof ObjectNode serviceObj) {
            serviceObj.set("image", new TextNode(image));
        }
    }

    public static JsonNode findServiceByImage(JsonNode compose, Pattern imagePattern) {
        Map.Entry<String, JsonNode> entry = findServiceEntryByImage(compose, imagePattern);
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Reads the name (service key) of the first service whose image matches the given pattern.
     *
     * @return the service name, or {@code null} if no matching service is found
     */
    public static String findServiceNameByImage(JsonNode compose, Pattern imagePattern) {
        Map.Entry<String, JsonNode> entry = findServiceEntryByImage(compose, imagePattern);
        return entry != null ? entry.getKey() : null;
    }

    /**
     * Resolves the name of the <b>first</b> service whose image matches each of the given regex patterns, preserving
     * pattern order and de-duplicating across patterns. One pattern therefore contributes at most one service name: a
     * pattern is expected to identify a single service, so matching several means the pattern is too broad.
     *
     * @return a comma-separated list of matched service names, or an empty string when no patterns are given or none match
     */
    public static String resolveServiceNames(JsonNode compose, @Nullable List<String> serviceImageRegexPatterns) {
        if (serviceImageRegexPatterns == null) {
            return "";
        }
        Set<String> names = new LinkedHashSet<>();
        for (String pattern : serviceImageRegexPatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String name = findServiceNameByImage(compose, Pattern.compile(pattern));
            if (name != null) {
                names.add(name);
            }
        }
        return String.join(",", names);
    }

    private static Map.Entry<String, JsonNode> findServiceEntryByImage(JsonNode compose, Pattern imagePattern) {
        JsonNode services = getServices(compose, imagePattern);
        if (services == null) {
            return null;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            JsonNode service = entry.getValue();
            if (service.isObject() && service.has("image")
                    && imagePattern.matcher(service.get("image").asText()).matches()) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Sets environment variables on the first matching service.
     * Only overwrites keys that already exist in the environment node.
     */
    public static void setEnvVariables(JsonNode compose, Pattern imagePattern, Map<String, String> envVars) {
        if (envVars.isEmpty()) {
            return;
        }
        JsonNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        if (env.isObject()) {
            ObjectNode obj = (ObjectNode) env;
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                if (obj.has(entry.getKey())) {
                    obj.set(entry.getKey(), new TextNode(entry.getValue()));
                }
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            int idx = findArrayEntryIndex(arr, entry.getKey());
            if (idx >= 0) {
                arr.set(idx, new TextNode(entry.getKey() + "=" + entry.getValue()));
            }
        }
    }

    /**
     * Sets environment variables on the first matching service, inserting missing keys.
     */
    public static void upsertEnvVariables(JsonNode compose, Pattern imagePattern, Map<String, String> envVars) {
        if (envVars.isEmpty()) {
            return;
        }
        JsonNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        if (env.isObject()) {
            ObjectNode obj = (ObjectNode) env;
            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                obj.set(entry.getKey(), new TextNode(entry.getValue()));
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            int idx = findArrayEntryIndex(arr, entry.getKey());
            String value = entry.getKey() + "=" + entry.getValue();
            if (idx >= 0) {
                arr.set(idx, new TextNode(value));
            } else {
                arr.add(new TextNode(value));
            }
        }
    }

    /**
     * Compare two compose documents for semantic equality while ignoring the
     * given env keys on the main service (identified by {@code imagePattern}).
     * Both inputs are deep-copied, so the originals are not modified.
     */
    public static boolean equalsIgnoringEnvKeys(JsonNode a, JsonNode b, Pattern imagePattern, List<String> envKeysToIgnore) {
        if (a == b) {
            return true;
        }
        JsonNode aCopy = a != null ? a.deepCopy() : null;
        JsonNode bCopy = b != null ? b.deepCopy() : null;
        if (imagePattern != null && envKeysToIgnore != null && !envKeysToIgnore.isEmpty()) {
            removeEnvVariables(aCopy, imagePattern, envKeysToIgnore);
            removeEnvVariables(bCopy, imagePattern, envKeysToIgnore);
        }
        return Objects.equals(aCopy, bCopy);
    }

    /** Removes the listed keys from the first matching service's environment. */
    public static void removeEnvVariables(JsonNode compose, Pattern imagePattern, List<String> keys) {
        JsonNode env = findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            return;
        }
        if (env.isObject()) {
            ObjectNode obj = (ObjectNode) env;
            for (String key : keys) {
                obj.remove(key);
            }
            return;
        }
        ArrayNode arr = (ArrayNode) env;
        for (String key : keys) {
            int idx = findArrayEntryIndex(arr, key);
            if (idx >= 0) {
                arr.remove(idx);
            }
        }
    }

    /** True when the env node (map or list) declares the given key. */
    public static boolean envHasKey(JsonNode env, String key) {
        if (env == null) {
            return false;
        }
        if (env.isObject()) {
            return env.has(key);
        }
        if (env.isArray()) {
            return findArrayEntryIndex((ArrayNode) env, key) >= 0;
        }
        return false;
    }

    /** Reads a value from either a map-form or list-form env node. */
    public static String envGet(JsonNode env, String key) {
        if (env == null) {
            return null;
        }
        if (env.isObject()) {
            return env.has(key) ? env.get(key).asText() : null;
        }
        if (env.isArray()) {
            int idx = findArrayEntryIndex((ArrayNode) env, key);
            if (idx < 0) {
                return null;
            }
            String entry = env.get(idx).asText();
            int eq = entry.indexOf('=');
            return eq >= 0 ? entry.substring(eq + 1) : "";
        }
        return null;
    }

    /**
     * Collects every relative host path the compose document references. The compose runs on a remote host
     * whose working directory the server does not control, so all of these break (or bind the wrong path) at
     * deploy time. Three locations are scanned:
     * <ul>
     *     <li>{@code services.*.volumes} — short syntax ({@code "source:target[:mode]"}) and long syntax
     *     ({@code {type: bind, source: ..., target: ...}});</li>
     *     <li>top-level {@code configs}/{@code secrets} with {@code file: ./nginx.conf};</li>
     *     <li>top-level {@code volumes.<name>.driver_opts.device} (the named-volume-backed-by-bind form).</li>
     * </ul>
     * Named volumes, absolute host paths, anonymous volumes (container-only) and variable references
     * ({@code $...}) are not reported.
     *
     * @return the list of offending relative source paths (empty when none)
     */
    public static List<String> findRelativeVolumeSources(JsonNode compose) {
        List<String> relative = new ArrayList<>();
        collectRelativeServiceVolumeSources(compose, relative);
        collectRelativeFileReferences(compose, "configs", relative);
        collectRelativeFileReferences(compose, "secrets", relative);
        collectRelativeDriverOptDevices(compose, relative);
        return relative;
    }

    private static void collectRelativeServiceVolumeSources(JsonNode compose, List<String> relative) {
        JsonNode services = getServices(compose);
        if (services == null) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = services.fields();
        while (it.hasNext()) {
            JsonNode service = it.next().getValue();
            if (!service.isObject()) {
                continue;
            }
            JsonNode volumes = service.get("volumes");
            if (volumes == null || !volumes.isArray()) {
                continue;
            }
            for (JsonNode volume : volumes) {
                String source = extractVolumeSource(volume);
                if (isRelativeHostPath(source)) {
                    relative.add(source);
                }
            }
        }
    }

    private static void collectRelativeFileReferences(JsonNode compose, String section, List<String> relative) {
        JsonNode sectionNode = compose != null ? compose.get(section) : null;
        if (sectionNode == null || !sectionNode.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = sectionNode.fields();
        while (it.hasNext()) {
            JsonNode entry = it.next().getValue();
            if (entry == null || !entry.isObject()) {
                continue;
            }
            JsonNode file = entry.get("file");
            if (file != null && file.isTextual() && isRelativeHostPath(file.asText())) {
                relative.add(file.asText());
            }
        }
    }

    private static void collectRelativeDriverOptDevices(JsonNode compose, List<String> relative) {
        JsonNode volumes = compose != null ? compose.get("volumes") : null;
        if (volumes == null || !volumes.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = volumes.fields();
        while (it.hasNext()) {
            JsonNode volume = it.next().getValue();
            if (volume == null || !volume.isObject()) {
                continue;
            }
            JsonNode driverOpts = volume.get("driver_opts");
            if (driverOpts == null || !driverOpts.isObject()) {
                continue;
            }
            JsonNode device = driverOpts.get("device");
            if (device != null && device.isTextual() && isRelativeHostPath(device.asText())) {
                relative.add(device.asText());
            }
        }
    }

    private static String extractVolumeSource(JsonNode volume) {
        if (volume == null) {
            return null;
        }
        if (volume.isTextual()) {
            String text = volume.asText();
            int colon = text.indexOf(':');
            // no colon -> anonymous volume (container path only), nothing to bind from the host
            return colon > 0 ? text.substring(0, colon) : null;
        }
        if (volume.isObject()) {
            JsonNode type = volume.get("type");
            // only bind mounts reference a host path; volume/tmpfs/npipe never do
            if (type != null && !"bind".equals(type.asText())) {
                return null;
            }
            JsonNode source = volume.get("source");
            return source != null ? source.asText() : null;
        }
        return null;
    }

    static boolean isRelativeHostPath(String source) {
        if (source == null || source.isBlank()) {
            return false;
        }
        // Allowed: variable references ('$...', resolved by the engine), absolute paths and named
        // volumes (a bare token with no path separator). Everything else non-absolute is a relative
        // host path: a leading '.' ('./', '../') or '~', or any source that carries a path separator
        // (e.g. 'data/logs', 'sub/../../etc') without being absolute.
        if (source.startsWith("$")) {
            return false;
        }
        if (isAbsoluteHostPath(source)) {
            return false;
        }
        return source.startsWith(".") || source.startsWith("~")
                || source.indexOf('/') >= 0 || source.indexOf('\\') >= 0;
    }

    private static boolean isAbsoluteHostPath(String source) {
        if (source.startsWith("/")) {
            return true;
        }
        // Windows absolute path, e.g. 'C:\...' or 'C:/...'.
        return source.length() >= 3 && Character.isLetter(source.charAt(0))
                && source.charAt(1) == ':' && (source.charAt(2) == '\\' || source.charAt(2) == '/');
    }

    private static JsonNode getServices(JsonNode compose, Pattern imagePattern) {
        return imagePattern != null ? getServices(compose) : null;
    }

    private static JsonNode getServices(JsonNode compose) {
        if (compose == null || compose.isNull()) {
            return null;
        }
        JsonNode services = compose.get("services");
        return services != null && services.isObject() ? services : null;
    }

    private static int findArrayEntryIndex(ArrayNode arr, String key) {
        String prefix = key + "=";
        for (int i = 0; i < arr.size(); i++) {
            JsonNode item = arr.get(i);
            if (item != null && item.isTextual()) {
                String v = item.asText();
                if (v.equals(key) || v.startsWith(prefix)) {
                    return i;
                }
            }
        }
        return -1;
    }

}
