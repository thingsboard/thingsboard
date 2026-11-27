// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Extension infrastructure mode: the single switch that selects the deployment substrate and its
 * sensible defaults. It is the one source of truth for whether the feature is on and which backend
 * runs the containers. The lower-level {@code extensions.*} knobs (k8s cluster mode, slug suffix,
 * network policy, ...) stay available as advanced overrides on top of the mode defaults.
 *
 * <ul>
 *   <li>{@link #DISABLED} — the whole feature is off (no controller, no reconciler).</li>
 *   <li>{@link #DOCKER_COMPOSE} — run each extension as a container on a Docker Engine (single host).</li>
 *   <li>{@link #PRIVATE_CLOUD} — Kubernetes, one cluster shared with TB core (extensions in their own
 *       namespace). No forced slug suffix.</li>
 *   <li>{@link #PUBLIC_CLOUD} — Kubernetes, a separate isolated cluster for untrusted tenant code.
 *       Slug carries a random suffix for system-wide uniqueness and unguessability.</li>
 * </ul>
 *
 * <p>The string forms below (kebab-case) are the values used in {@code extensions.mode} and in the
 * bean {@code @ConditionalOnExpression} gates.</p>
 */
public enum ExtensionMode {

    DISABLED("disabled"),
    DOCKER_COMPOSE("docker-compose"),
    PRIVATE_CLOUD("private-cloud"),
    PUBLIC_CLOUD("public-cloud");

    // Bean-activation SpEL expressions over the extensions.mode property, kept here as the single source
    // of truth so the same classification is not hand-copied across every @ConditionalOnExpression. They
    // are compile-time String constants (literals), which annotation attributes require — do NOT try to
    // build them from the instance key field. The comparisons are trimmed and case-insensitive so they
    // match fromKey()'s normalization exactly: a value like 'Docker-Compose' or 'DISABLED' activates the
    // same beans it parses to, instead of enabling the feature with no reconciler (which fails startup).
    // Blank counts as disabled too: the ':disabled' default applies only when the property is missing,
    // so 'TB_EXTENSIONS_MODE=' (present but empty) resolves to '' — which fromKey() maps to DISABLED.
    /** Feature on: any non-blank, non-disabled mode (controller, status poller, reconcile loop). */
    public static final String WHEN_ENABLED = "!'${extensions.mode:disabled}'.trim().isEmpty() and !'${extensions.mode:disabled}'.trim().equalsIgnoreCase('disabled')";
    /** Kubernetes substrate: private-cloud or public-cloud. */
    public static final String WHEN_KUBERNETES = "'${extensions.mode:disabled}'.trim().equalsIgnoreCase('private-cloud') or '${extensions.mode:disabled}'.trim().equalsIgnoreCase('public-cloud')";
    /** Docker (docker-compose) substrate. */
    public static final String WHEN_DOCKER = "'${extensions.mode:disabled}'.trim().equalsIgnoreCase('docker-compose')";

    private final String key;

    ExtensionMode(String key) {
        this.key = key;
    }

    @JsonValue
    public String getKey() {
        return key;
    }

    /**
     * Whether the immutable per-extension slug must carry a random {@code -<4hex>} suffix. Only
     * public cloud forces it (many untrusted tenants share one slug space); the single-tenant /
     * trusted modes let the operator keep the exact slug they typed.
     */
    public boolean enforcesSlugSuffix() {
        return this == PUBLIC_CLOUD;
    }

    /**
     * Whether the substrate actually runs the replica count the spec asks for. A Docker host has no
     * load balancer behind a single container name, so that backend treats {@code replicas} as on/off
     * and runs exactly one container whatever the number says. Quota must charge for what is really
     * scheduled: billing replicas the substrate never starts rejects deploys while the capacity sits
     * physically idle, and reports usage the operator cannot reconcile with the running containers.
     */
    public boolean runsRequestedReplicas() {
        return this != DOCKER_COMPOSE;
    }

    @JsonCreator
    public static ExtensionMode fromKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DISABLED;
        }
        String normalized = value.trim().toLowerCase();
        for (ExtensionMode mode : values()) {
            if (mode.key.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown extensions.mode '" + value + "'. Expected one of: "
                + "disabled, docker-compose, private-cloud, public-cloud.");
    }

}
