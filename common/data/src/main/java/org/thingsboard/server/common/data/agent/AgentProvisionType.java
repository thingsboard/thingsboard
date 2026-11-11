// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import lombok.Getter;

@Getter
public enum AgentProvisionType {

    /**
     * Agent self-provisioning is disabled; agents can only be created manually.
     * No applications are installed automatically.
     */
    DISABLED(false),

    /**
     * Agents can self-provision using the profile's provision key/secret,
     * but no applications are installed automatically — assigned application
     * profiles are ignored during auto-install.
     */
    NO_AUTO_INSTALL(false),

    /**
     * Agents can self-provision. On initial sync, applications are auto-installed
     * from the assigned application profiles: Edge/Gateway profiles install only if
     * the agent has no application of that type yet (at most one per type per agent,
     * the earliest-assigned profile wins); Generic profiles install once per profile.
     */
    AUTO_INSTALL_PER_APP_TYPE(true),

    /**
     * Agents can self-provision. On initial sync, every assigned application profile
     * is auto-installed unless the agent already has a matching application:
     * Edge/Gateway applications match by same-type template version or by their
     * source profile (one per template per agent); Generic applications match
     * by profile (one application per profile per agent).
     */
    AUTO_INSTALL_PER_APP_PROFILE(true);

    private final boolean appAutoInstallSupported;

    AgentProvisionType(boolean appAutoInstallSupported) {
        this.appAutoInstallSupported = appAutoInstallSupported;
    }
}
