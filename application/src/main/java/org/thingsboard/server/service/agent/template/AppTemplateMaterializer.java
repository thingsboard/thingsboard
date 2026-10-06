// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.template;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbVersionUtils;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.config.TemplateVarSubstitutor;
import org.thingsboard.server.common.data.agent.step.AgentAppStep;
import org.thingsboard.server.common.data.agent.step.ComposeTypeChoiceStep;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.StepLinkedListUtils;
import org.thingsboard.server.dao.edge.EdgeEditionStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands a single abstract app template (raw JSON with {@code ${var.*}} placeholders and {@code condition} steps)
 * into concrete per-version {@link AgentAppTemplate}s, one per entry in the version graph. Variable substitution runs
 * at the JSON level (see {@link TemplateVarSubstitutor}) before deserialization so boolean/number placeholders land in
 * typed positions; conditional steps are then dropped and the {@code nextId} chain re-stitched.
 *
 * <p>Compose bodies are loaded via a caller-supplied {@code composeLoader} (path -> parsed compose JSON), keeping this
 * component free of git/YAML concerns. App types with no version graph (GENERIC) supply a single default-version descriptor.
 */
@Component
@Slf4j
public class AppTemplateMaterializer {

    private static final Pattern LEADING_VERSION = Pattern.compile("^(\\d+)(?:\\.(\\d+))?");
    // Gateway "latest" = the newest '<major>.<minor>-stable' tagged version; the graph head is only a fallback.
    private static final Pattern GATEWAY_STABLE_VERSION = Pattern.compile(".*-stable");

    /**
     * One node of the version graph for an app type, sourced from {@code edge-update.json} / {@code gw-update.json}.
     */
    @Data
    @AllArgsConstructor
    public static class AppVersionDescriptor {
        private final String version;
        private final String nextVersion;
        private final boolean requiresUpdateDb;
    }

    /**
     * Materialize every version for one app type and return the per-version map plus the head (install) template.
     * For GATEWAY the head is the newest '*-stable' tagged version when one exists; otherwise (and for the other
     * app types) it is the version whose {@code nextVersion} is null.
     * Returns an empty result when {@code descriptors} is empty (e.g. the version graph could not be fetched).
     */
    public MaterializationResult materialize(JsonNode rawTemplate,
                                             AgentApplicationType appType,
                                             AgentAppConfigType configType,
                                             List<AppVersionDescriptor> descriptors,
                                             Function<String, JsonNode> composeLoader) {
        Map<String, AgentAppTemplate> byVersion = new LinkedHashMap<>();
        AgentAppTemplate latest = null;

        if (descriptors == null || descriptors.isEmpty()) {
            return new MaterializationResult(byVersion, null);
        }

        for (AppVersionDescriptor d : descriptors) {
            try {
                Map<String, Object> vars = buildVars(d);
                AgentAppTemplate template = materializeOne(rawTemplate, appType, configType,
                        d.getVersion(), d.getNextVersion(), vars, composeLoader);
                byVersion.put(d.getVersion(), template);
                if (d.getNextVersion() == null) {
                    latest = template;
                }
            } catch (Exception e) {
                log.warn("Error while materializing template for version {}", d.getVersion(), e);
            }
        }
        if (appType == AgentApplicationType.GATEWAY) {
            latest = latestStableVersion(byVersion).orElse(latest);
        }
        if (latest == null && !byVersion.isEmpty()) {
            // Fallback: no explicit head (null nextVersion) found; use the last descriptor.
            latest = byVersion.values().stream().reduce((a, b) -> b).orElse(null);
        }
        return new MaterializationResult(byVersion, latest);
    }

    private static Optional<AgentAppTemplate> latestStableVersion(Map<String, AgentAppTemplate> byVersion) {
        return byVersion.values().stream()
                .filter(t -> t.getCurrentVersion() != null && GATEWAY_STABLE_VERSION.matcher(t.getCurrentVersion()).matches())
                .max(Comparator.comparing(
                        (AgentAppTemplate t) -> TbVersionUtils.extractStartingDigits(t.getCurrentVersion()),
                        TbVersionUtils::compare)
                        .thenComparing(AgentAppTemplate::getCurrentVersion));
    }

    private AgentAppTemplate materializeOne(JsonNode rawTemplate,
                                            AgentApplicationType appType,
                                            AgentAppConfigType configType,
                                            String version,
                                            String nextVersion,
                                            Map<String, Object> vars,
                                            Function<String, JsonNode> composeLoader) {

        JsonNode substituted = TemplateVarSubstitutor.substitute(rawTemplate, vars);
        AgentAppTemplate template = JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER
                .convertValue(substituted, AgentAppTemplate.class);

        template.setTenantId(TenantId.SYS_TENANT_ID);
        template.setAppType(appType);
        template.setConfigType(configType);
        template.setCurrentVersion(version);
        template.setNextVersion(nextVersion);

        resolveComposeTemplates(template.getStartSteps(), vars, composeLoader);

        Predicate<AgentAppStep> disabled = step ->
                step.getCondition() != null && !truthy(vars.get(step.getCondition()));
        template.setStartSteps(StepLinkedListUtils.removeAndRestitch(template.getStartSteps(), disabled));
        template.setUpgradeSteps(StepLinkedListUtils.removeAndRestitch(template.getUpgradeSteps(), disabled));
        template.setDeleteSteps(StepLinkedListUtils.removeAndRestitch(template.getDeleteSteps(), disabled));
        template.setRollbackSteps(StepLinkedListUtils.removeAndRestitch(template.getRollbackSteps(), disabled));
        template.setRestartSteps(StepLinkedListUtils.removeAndRestitch(template.getRestartSteps(), disabled));
        return template;
    }

    private void resolveComposeTemplates(List<AgentAppStep> steps, Map<String, Object> vars,
                                         Function<String, JsonNode> composeLoader) {
        if (steps == null) {
            return;
        }
        for (AgentAppStep step : steps) {
            if (!(step instanceof ComposeTypeChoiceStep choice) || choice.getComposeTemplates() == null) {
                continue;
            }
            Map<String, JsonNode> resolved = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : choice.getComposeTemplates().entrySet()) {
                JsonNode value = entry.getValue();
                if (value != null && value.isTextual()) {
                    String path = value.asText();
                    if (path.endsWith(".yml") || path.endsWith(".yaml")) {
                        JsonNode compose = loadMostSpecificCompose(path, vars, composeLoader);
                        resolved.put(entry.getKey(), TemplateVarSubstitutor.substitute(compose, vars));
                        continue;
                    }
                }
                resolved.put(entry.getKey(), value);
            }
            choice.setComposeTemplates(resolved);
        }
    }

    /**
     * Load the compose body for {@code path}, preferring the most specific version subfolder that actually exists in
     * the repo. For version {@code 4.4.3} with a base {@code composeLine} of {@code 4.4}, this tries
     * {@code compose/edge/4.4/4.4.3/…}, then the lower siblings {@code …/4.4/4.4.2/…}, {@code …/4.4/4.4.1/…},
     * {@code …/4.4/4.4.0/…}, then the base {@code …/4.4/…}. Falls through to an error (so the version is skipped,
     * as before) only when no candidate exists.
     */
    private JsonNode loadMostSpecificCompose(String path, Map<String, Object> vars, Function<String, JsonNode> composeLoader) {
        List<String> candidates = composePathCandidates(path, vars);
        for (String candidate : candidates) {
            JsonNode compose = composeLoader.apply(candidate);
            if (compose != null) {
                return compose;
            }
        }
        throw new IllegalArgumentException("No compose file found for path " + path + " (tried " + candidates + ")");
    }

    /**
     * Candidate compose paths, most specific first. The {@code composeLine} folder of {@code path} is swapped for the
     * nested version subfolders of {@link #subfolderVersions}, ending with {@code path} itself, e.g. for version
     * {@code 4.4.3}: {@code …/4.4/4.4.3/…}, {@code …/4.4/4.4.2/…}, {@code …/4.4/4.4.1/…}, {@code …/4.4/4.4.0/…},
     * {@code …/4.4/…}.
     */
    static List<String> composePathCandidates(String path, Map<String, Object> vars) {
        Object line = vars.get("composeLine");
        Object version = vars.get("version");
        if (line == null || version == null) {
            return List.of(path);
        }
        String lineFolder = "/" + line + "/";
        List<String> candidates = new ArrayList<>();
        for (String subfolderVersion : subfolderVersions(line.toString(), TbVersionUtils.extractStartingDigits(version.toString()))) {
            String subfolder = "/" + nestedFolder(line.toString(), subfolderVersion) + "/";
            candidates.add(path.replaceFirst(Pattern.quote(lineFolder), Matcher.quoteReplacement(subfolder)));
        }
        candidates.add(path);
        return candidates;
    }

    /**
     * Versions whose subfolder may hold the compose for {@code version}, most specific first. A subfolder applies to
     * its own version and every later sibling until a newer one exists, so each level is followed by its lower
     * siblings: {@code 4.4.1.2} -> {@code 4.4.1.2, 4.4.1.1, 4.4.1.0, 4.4.1, 4.4.0}. Empty when the version is the line
     * itself or does not belong to it.
     */
    private static List<String> subfolderVersions(String line, String version) {
        List<String> versions = new ArrayList<>();
        if (!version.startsWith(line + ".")) {
            return versions;
        }
        for (String level = version; !level.equals(line); level = parentVersion(level)) {
            String parent = parentVersion(level);
            for (int sibling = lastSegment(level); sibling >= 0; sibling--) {
                versions.add(parent + "." + sibling);
            }
        }
        return versions;
    }

    // Nested folder of a version under its line, e.g. line 4.4 and version 4.4.1.2 -> "4.4/4.4.1/4.4.1.2".
    private static String nestedFolder(String line, String version) {
        List<String> folders = new ArrayList<>();
        for (String level = version; !level.equals(line); level = parentVersion(level)) {
            folders.add(0, level);
        }
        folders.add(0, line);
        return String.join("/", folders);
    }

    private static String edgeRepo(String version) {
        return version == null ? null : EdgeEditionStyle.getEdgeEditionStyle(version).getDockerRepo();
    }

    private static String parentVersion(String version) {
        return version.substring(0, version.lastIndexOf('.'));
    }

    private static int lastSegment(String version) {
        return Integer.parseInt(version.substring(version.lastIndexOf('.') + 1));
    }

    private Map<String, Object> buildVars(AppVersionDescriptor d) {
        Map<String, Object> vars = new HashMap<>();
        // App-agnostic + per-app aliases, so the same materializer serves EDGE/GATEWAY templates.
        vars.put("version", d.getVersion());
        vars.put("edgeVersion", d.getVersion());
        vars.put("gatewayVersion", d.getVersion());
        vars.put("nextVersion", d.getNextVersion());
        vars.put("nextEdgeVersion", d.getNextVersion());
        vars.put("nextGatewayVersion", d.getNextVersion());
        // edge image repo per version, as CE-style versions are published under tb-edge instead of tb-edge-pe
        vars.put("edgeRepo", edgeRepo(d.getVersion()));
        vars.put("nextEdgeRepo", edgeRepo(d.getNextVersion()));
        vars.put("composeLine", composeLine(d.getVersion()));
        vars.put("requiresUpdateDb", d.isRequiresUpdateDb());
        return vars;
    }

    /**
     * major.minor of the leading numeric part of a version (e.g. {@code 4.3.1.2EDGEPE -> 4.3}, {@code 3.8-stable -> 3.8}).
     */
    static String composeLine(String version) {
        if (version == null) {
            return null;
        }
        Matcher m = LEADING_VERSION.matcher(version);
        if (!m.find()) {
            return version;
        }
        String major = m.group(1);
        String minor = m.group(2);
        return minor != null ? major + "." + minor : major;
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    @Data
    @AllArgsConstructor
    public static class MaterializationResult {
        private final Map<String, AgentAppTemplate> byVersion;
        private final AgentAppTemplate latest;
    }
}
