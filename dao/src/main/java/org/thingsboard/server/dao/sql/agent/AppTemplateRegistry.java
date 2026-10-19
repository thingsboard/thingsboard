// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.agent.template.AgentAppTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of materialized agent-app templates, replacing the {@code agent_app_template} DB table. Holds one
 * concrete {@link AgentAppTemplate} per (appType, configType, version); templates are produced by the materializer from
 * a single abstract template plus the version graph, and reference is by version string (see the abstract-templates
 * refactor). Unversioned app types (GENERIC) are registered under {@link AgentApplicationType#getDefaultVersion()}.
 *
 * <p>Refreshes swap whole per-(appType, configType) maps atomically, so lookups never observe a half-populated key.
 */
@Component
@Slf4j
public class AppTemplateRegistry {

    private final Map<Key, Map<String, AgentAppTemplate>> byVersion = new ConcurrentHashMap<>();
    private final Map<Key, AgentAppTemplate> latestByKey = new ConcurrentHashMap<>();

    /**
     * Atomically replace all templates for one (appType, configType). {@code latest} is the install/head template
     * (typically the version whose {@code nextVersion} is null).
     */
    public void replace(AgentApplicationType appType, AgentAppConfigType configType,
                        Map<String, AgentAppTemplate> versions, AgentAppTemplate latest) {
        Key key = new Key(appType, configType);
        byVersion.put(key, Collections.unmodifiableMap(new HashMap<>(versions)));
        if (latest != null) {
            latestByKey.put(key, latest);
        } else {
            latestByKey.remove(key);
        }
        log.info("Registered {} template version(s) for app type {} / config type {}", versions.size(), appType, configType);
    }

    /**
     * Drop every registered (appType, configType) that is not in {@code keep} — the reconciliation half of a sync pass,
     * so a template withdrawn upstream stops being handed to agents without waiting for a restart. A no-op when
     * {@code keep} is empty, since an empty pass means the source could not be read rather than that everything was
     * withdrawn.
     */
    public void retainOnly(Set<Key> keep) {
        if (keep.isEmpty()) {
            return;
        }
        byVersion.keySet().removeIf(key -> {
            boolean stale = !keep.contains(key);
            if (stale) {
                latestByKey.remove(key);
                log.info("Dropped stale templates for app type {} / config type {}", key.appType(), key.configType());
            }
            return stale;
        });
    }

    /**
     * Look up the concrete template for the given app type, config type and version. Returns null when the exact
     * version is not registered — callers that want the head template should use {@link #latest} explicitly.
     */
    public AgentAppTemplate get(AgentApplicationType appType, AgentAppConfigType configType, String version) {
        if (version == null) {
            return null;
        }
        Map<String, AgentAppTemplate> versions = byVersion.get(new Key(appType, configType));
        return versions == null ? null : versions.get(version);
    }

    /**
     * The install/head template for the (appType, configType), or null if none registered.
     */
    public AgentAppTemplate latest(AgentApplicationType appType, AgentAppConfigType configType) {
        return latestByKey.get(new Key(appType, configType));
    }

    /**
     * The next version in the upgrade chain for {@code version}, or null if it is the head (or unknown).
     */
    public String next(AgentApplicationType appType, AgentAppConfigType configType, String version) {
        AgentAppTemplate template = get(appType, configType, version);
        return template != null ? template.getNextVersion() : null;
    }

    /**
     * All templates registered for an (appType, configType) (empty if none).
     */
    public List<AgentAppTemplate> list(AgentApplicationType appType, AgentAppConfigType configType) {
        Map<String, AgentAppTemplate> versions = byVersion.get(new Key(appType, configType));
        return versions == null ? List.of() : new ArrayList<>(versions.values());
    }

    /**
     * All templates across every app type and config type.
     */
    public List<AgentAppTemplate> all() {
        List<AgentAppTemplate> result = new ArrayList<>();
        for (Map<String, AgentAppTemplate> versions : byVersion.values()) {
            result.addAll(versions.values());
        }
        return result;
    }

    public boolean isEmpty() {
        return byVersion.isEmpty();
    }

    public record Key(AgentApplicationType appType, AgentAppConfigType configType) {
    }

}
