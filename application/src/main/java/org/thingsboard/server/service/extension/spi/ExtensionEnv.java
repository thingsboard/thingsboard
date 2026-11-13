// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import org.thingsboard.server.common.data.extension.Extension;

import java.util.LinkedHashMap;

/**
 * Single source of truth for the platform-injected ({@code TB_EXT_*}) environment an extension
 * container receives, used by every substrate so an extension sees the same built-in variables in the
 * same order on Kubernetes and on Docker. Each backend only adapts these ordered entries to its own
 * representation (K8s {@code EnvVar}, Docker {@code KEY=VALUE} strings); the names, order and the
 * null-id fallbacks live here so they cannot drift between backends.
 */
public final class ExtensionEnv {

    /** Default base path the router exposes extensions under; the per-extension prefix is base/{slug}. */
    public static final String DEFAULT_BASE_PATH = "/api/extension/route";

    private ExtensionEnv() {}

    /**
     * The built-in env injected by the platform, in deterministic order. A {@code null} extension id /
     * tenant id maps to an empty string (the extension may be rendered before it is persisted).
     */
    public static LinkedHashMap<String, String> builtIns(Extension extension, String basePath, String tbInternalUrl) {
        String slug = extension.getSlug();
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("TB_EXT_SLUG", slug);
        env.put("TB_EXT_ID", extension.getId() != null ? extension.getId().getId().toString() : "");
        env.put("TB_EXT_TENANT_ID", extension.getTenantId() != null ? extension.getTenantId().getId().toString() : "");
        env.put("TB_EXT_BASE_PATH", basePath + "/" + slug);
        env.put("THINGSBOARD_URL", tbInternalUrl);
        return env;
    }

}
