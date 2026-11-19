// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.spi;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.RegistryAuth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared naming + label scheme for a managed extension workload, used by every substrate so a slug
 * maps to the same object/container name ({@code tb-ext-{slug}}) and carries the same identifying
 * labels everywhere. The label KEYS follow the Kubernetes recommended-label convention
 * ({@code app.kubernetes.io/*}); Docker reuses them verbatim as container labels (Docker labels are
 * arbitrary key/value), which keeps the managed-by / slug markers — and therefore the orphan-sweep
 * query — identical across substrates.
 */
public final class ExtensionResourceNames {

    public static final String NAME_PREFIX = "tb-ext-";
    public static final String ENV_SUFFIX = "-env";
    public static final String PULL_SUFFIX = "-pull";

    public static final String APP_LABEL = "tb-extension";
    public static final String LABEL_APP_NAME = "app.kubernetes.io/name";
    public static final String LABEL_APP_INSTANCE = "app.kubernetes.io/instance";
    public static final String LABEL_MANAGED_BY = "app.kubernetes.io/managed-by";
    public static final String MANAGED_BY = "thingsboard";
    public static final String LABEL_TENANT_ID = "tb.thingsboard.io/tenant-id";
    public static final String LABEL_EXTENSION_ID = "tb.thingsboard.io/extension-id";
    public static final String LABEL_EXTENSION_SLUG = "tb.thingsboard.io/extension-slug";

    public static final String ANNOTATION_SPEC_HASH = "tb.thingsboard.io/spec-hash";
    public static final String ANNOTATION_RESTARTED_AT = "tb.thingsboard.io/restarted-at";

    public static final String CONTAINER_NAME = "extension";
    public static final int SERVICE_PORT = 8090;

    private ExtensionResourceNames() {}

    public static String deployment(String slug) {
        return NAME_PREFIX + slug;
    }

    public static String envSecret(String slug) {
        return NAME_PREFIX + slug + ENV_SUFFIX;
    }

    /** Managed dockerconfigjson Secret holding the resolved private-registry pull credentials. */
    public static String pullSecret(String slug) {
        return NAME_PREFIX + slug + PULL_SUFFIX;
    }

    // Best-effort registry host from an image ref: the first path segment when it looks like a host
    // (contains '.' or ':' or is 'localhost'); otherwise Docker Hub's canonical index URL.
    public static String registryHost(String image) {
        int slash = image == null ? -1 : image.indexOf('/');
        if (slash > 0) {
            String first = image.substring(0, slash);
            if (first.contains(".") || first.contains(":") || first.equals("localhost")) {
                return first;
            }
        }
        return "https://index.docker.io/v1/";
    }

    // The registry host used for pull credentials: the explicit one when the tenant set it, otherwise
    // derived from the image reference. Shared by both substrates so the rule cannot drift.
    public static String resolveRegistry(RegistryAuth auth, String image) {
        return auth != null && StringUtils.isNotBlank(auth.getRegistry()) ? auth.getRegistry() : registryHost(image);
    }

    public static String service(String slug) {
        return NAME_PREFIX + slug;
    }

    public static String networkPolicy(String slug) {
        return NAME_PREFIX + slug;
    }

    /** Container name on a Docker host (single container per extension). */
    public static String container(String slug) {
        return NAME_PREFIX + slug;
    }

    public static String instanceLabel(String slug) {
        return NAME_PREFIX + slug;
    }

    public static Map<String, String> baseLabels(Extension extension) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LABEL_APP_NAME, APP_LABEL);
        labels.put(LABEL_APP_INSTANCE, instanceLabel(extension.getSlug()));
        labels.put(LABEL_MANAGED_BY, MANAGED_BY);
        if (extension.getTenantId() != null && extension.getTenantId().getId() != null) {
            labels.put(LABEL_TENANT_ID, extension.getTenantId().getId().toString());
        }
        if (extension.getId() != null) {
            labels.put(LABEL_EXTENSION_ID, extension.getId().getId().toString());
        }
        labels.put(LABEL_EXTENSION_SLUG, extension.getSlug());
        return labels;
    }

    /**
     * True when these labels identify the workload as belonging to THIS extension: managed by us and
     * stamped with the same tenant id and extension id.
     *
     * <p>Every object name derives from the slug, and a slug is released the moment the row is deleted
     * while the teardown of its workload runs asynchronously — and is skipped by the orphan sweep once a
     * new row owns the slug. So a name (or the slug label) alone can still resolve to the PREVIOUS
     * owner's workload, which belongs to another tenant. Read paths use this to refuse a workload that
     * is not ours instead of reporting its state or its logs; apply uses it to recreate such a workload
     * instead of adopting it.
     */
    public static boolean ownedBy(Map<String, String> labels, Extension extension) {
        if (labels == null || extension == null
                || !MANAGED_BY.equals(labels.get(LABEL_MANAGED_BY))) {
            return false;
        }
        return equalsId(labels.get(LABEL_TENANT_ID), extension.getTenantId() != null ? extension.getTenantId().getId() : null)
                && equalsId(labels.get(LABEL_EXTENSION_ID), extension.getId() != null ? extension.getId().getId() : null);
    }

    // An absent id on either side is never a match: a workload with no owner stamp cannot be proven ours.
    private static boolean equalsId(String label, UUID id) {
        return label != null && id != null && label.equals(id.toString());
    }

    /** Label selector that matches only the workload of this extension; see {@link #ownedBy}. */
    public static Map<String, String> ownerSelector(Extension extension) {
        Map<String, String> selector = new LinkedHashMap<>();
        selector.put(LABEL_MANAGED_BY, MANAGED_BY);
        if (extension.getTenantId() != null && extension.getTenantId().getId() != null) {
            selector.put(LABEL_TENANT_ID, extension.getTenantId().getId().toString());
        }
        if (extension.getId() != null) {
            selector.put(LABEL_EXTENSION_ID, extension.getId().getId().toString());
        }
        return selector;
    }

    public static String annotation(Map<String, String> annotations, String key) {
        return annotations == null ? null : annotations.get(key);
    }

}
