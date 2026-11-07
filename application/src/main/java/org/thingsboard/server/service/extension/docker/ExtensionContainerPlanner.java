// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.docker;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.service.extension.spi.ExtensionEnv;
import org.thingsboard.server.service.extension.spi.ExtensionResourceNames;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker counterpart of {@code ExtensionManifestRenderer}: turns an {@link Extension} into a pure
 * {@link ContainerPlan}. No Docker client is touched here, so the mapping (env, labels, ports,
 * resource limits, pull policy, spec-hash) is fully unit-testable. Built-in env and the label scheme
 * are kept identical to the Kubernetes backend so an extension behaves the same on either substrate.
 */
public class ExtensionContainerPlanner {

    private final String tbInternalUrl;
    private final String basePath;

    public ExtensionContainerPlanner(String tbInternalUrl, String basePath) {
        this.tbInternalUrl = tbInternalUrl;
        this.basePath = StringUtils.isNotBlank(basePath) ? basePath : ExtensionEnv.DEFAULT_BASE_PATH;
    }

    // Operator-level inputs injected into every rendered container (env: THINGSBOARD_URL and
    // TB_EXT_BASE_PATH) but absent from the user spec. Folded into the spec-hash so changing the tb-core
    // URL or base path re-converges existing containers instead of leaving stale env behind.
    public String renderContext() {
        return tbInternalUrl + "\n" + basePath;
    }

    public ContainerPlan plan(Extension extension, String specHash) {
        ExtensionContainerSpec spec = extension.getContainerSpec();

        Map<String, String> labels = ExtensionResourceNames.baseLabels(extension);
        // Spec-hash lives in a label (Docker has no annotations); it is the re-apply idempotency marker.
        labels.put(ExtensionResourceNames.ANNOTATION_SPEC_HASH, specHash);

        ResourceSpec resources = spec.getResources() != null ? spec.getResources() : new ResourceSpec();

        return new ContainerPlan(
                ExtensionResourceNames.container(extension.getSlug()),
                spec.getImage(),
                spec.getTag(),
                "latest".equalsIgnoreCase(spec.getTag()),
                buildEnv(extension),
                labels,
                spec.getPort(),
                ResourceQuantities.memoryToBytes(resources.effectiveMemoryLimit()),
                ResourceQuantities.cpuToNanoCpus(resources.effectiveCpuLimit()),
                spec.getRegistryAuth(),
                specHash);
    }

    // Built-in env first (deterministic order), then user-supplied env. A user entry whose key equals
    // a built-in key is dropped so user env can never shadow a platform value: Docker env is
    // last-occurrence-wins, so without this filter a duplicate key would override the built-in (the
    // TB_EXT_ ones are also rejected by validation, but THINGSBOARD_URL is not prefixed).
    private List<String> buildEnv(Extension extension) {
        List<String> env = new ArrayList<>();
        LinkedHashMap<String, String> builtIns = ExtensionEnv.builtIns(extension, basePath, tbInternalUrl);
        builtIns.forEach((k, v) -> env.add(kv(k, v)));

        Map<String, String> userEnv = extension.getContainerSpec().getEnv();
        if (userEnv != null) {
            // Key-ordered so the rendered env list is stable regardless of map insertion order.
            Map<String, String> sorted = new LinkedHashMap<>();
            userEnv.entrySet().stream()
                    .filter(e -> !builtIns.containsKey(e.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sorted.put(e.getKey(), e.getValue()));
            sorted.forEach((k, v) -> env.add(kv(k, v != null ? v : "")));
        }
        return env;
    }

    private static String kv(String key, String value) {
        return key + "=" + value;
    }

}
