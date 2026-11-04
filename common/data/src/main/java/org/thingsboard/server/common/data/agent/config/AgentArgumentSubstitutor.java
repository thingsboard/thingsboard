// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${tb.<name>}} argument placeholders in a serialized compose/job body at RUNTIME, injecting the
 * tenant-resolved argument values for one install. This is the runtime counterpart to {@link TemplateVarSubstitutor}
 * (which resolves build-time {@code ${var.*}} placeholders against the version graph): this class runs later, operates
 * on the already-serialized JSON string, and decides raw-JSON vs escaped-string injection from each argument's declared
 * format. Placeholders with no resolved value are left intact so native docker-compose {@code ${VAR}} interpolation
 * still works.
 */
public class AgentArgumentSubstitutor {

    // Group 1 matches a placeholder that is the whole quoted value ("${tb.name}"); group 2 matches a
    // bare ${tb.name} placeholder embedded in a larger string. The whole-value alternative is tried
    // first so its surrounding quotes are consumed and can be dropped for raw JSON injection.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "\"\\$\\{tb\\.([a-zA-Z0-9_]+)}\"|\\$\\{tb\\.([a-zA-Z0-9_]+)}");

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private AgentArgumentSubstitutor() {
    }

    /**
     * Replaces ${tb.<name>} placeholders in the given (JSON-serialized) compose content with their resolved values.
     * <p>
     * For arguments with {@link AgentAppArgumentFormat#JSON}, when the placeholder is the whole value
     * (e.g. "ports": "${tb.ports}") and the resolved value is valid JSON, it is injected raw (the surrounding
     * quotes are replaced) so the compose keeps the structured type; if the value is not valid JSON it falls
     * back to a quoted string so the compose stays valid. In every other case (STRING format, a placeholder
     * embedded inside a larger string) the value is JSON-escaped and stays a string literal. Placeholders whose
     * name is not present in the resolved map are left untouched so native docker-compose ${VAR} interpolation
     * still works.
     */
    public static String substitute(String composeContent, Map<String, String> resolvedArguments,
                                    Collection<AgentAppArgument> arguments) {
        if (composeContent == null || composeContent.isEmpty() || resolvedArguments == null || resolvedArguments.isEmpty()) {
            return composeContent;
        }
        Set<String> jsonArgumentNames = jsonArgumentNames(arguments);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(composeContent);
        StringBuilder result = new StringBuilder();
        // appendReplacement advances over the original content, so a resolved value that itself
        // contains a ${tb.x} sequence is written to the output once and never substituted again.
        while (matcher.find()) {
            boolean wholeQuotedValue = matcher.group(1) != null;
            String name = wholeQuotedValue ? matcher.group(1) : matcher.group(2);
            String replacement = resolveReplacement(name, wholeQuotedValue, matcher.group(), resolvedArguments, jsonArgumentNames);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String resolveReplacement(String name, boolean wholeQuotedValue, String matched,
                                             Map<String, String> resolvedArguments, Set<String> jsonArgumentNames) {
        if (!resolvedArguments.containsKey(name)) {
            // Unknown placeholder: leave it untouched so native docker-compose ${VAR} interpolation still works.
            return matched;
        }
        String value = resolvedArguments.get(name);
        if (wholeQuotedValue && jsonArgumentNames.contains(name)) {
            JsonNode json = asJson(value);
            if (json != null) {
                // Valid JSON whole value -> inject raw (the match consumed the quotes) so compose keeps the structured type.
                return json.toString();
            }
        }
        String escaped = value == null ? "" : new String(JsonStringEncoder.getInstance().quoteAsString(value));
        // A whole-value match consumed the surrounding quotes, so re-add them; a bare placeholder had none.
        return wholeQuotedValue ? "\"" + escaped + "\"" : escaped;
    }

    private static Set<String> jsonArgumentNames(Collection<AgentAppArgument> arguments) {
        Set<String> names = new HashSet<>();
        if (arguments != null) {
            for (AgentAppArgument argument : arguments) {
                if (argument.isJsonFormat() && argument.getName() != null) {
                    names.add(argument.getName());
                }
            }
        }
        return names;
    }

    private static JsonNode asJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
