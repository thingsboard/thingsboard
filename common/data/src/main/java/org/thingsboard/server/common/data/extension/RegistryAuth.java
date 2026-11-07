// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * Private-registry credentials used to pull an extension image. The password is stored as a
 * ThingsBoard Secret reference ({@code ${secret:name;type:TEXT}}), never as plaintext: ThingsBoard
 * resolves it each time the extension is deployed — and passes it to the runtime (a managed
 * Kubernetes {@code dockerconfigjson} Secret, or a Docker Engine {@code AuthConfig} on pull).
 */
@Data
public class RegistryAuth implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Registry host the credentials authenticate to, e.g. registry.example.com. " +
            "Optional: when blank it is derived from the image reference.", example = "registry.example.com")
    private String registry;

    @Schema(description = "Registry username or robot-account name.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "robot$acme")
    private String username;

    // Holds a ${secret:...} reference, not a plaintext password — excluded from toString().
    @ToString.Exclude
    @Schema(description = "Registry password as a ThingsBoard Secret reference. Resolved at deploy time; " +
            "the plaintext value is never stored on the extension.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "${secret:acme-registry;type:TEXT}")
    private String password;

    /**
     * A copy carrying only the non-secret hints (registry + username), with the password left null, so
     * a list/projection response never echoes the secret reference back. Returns {@code null} for a
     * {@code null} source. Keeping this with the type ensures the "never copy the password" rule lives
     * next to the field it protects.
     */
    public static RegistryAuth redacted(RegistryAuth source) {
        if (source == null) {
            return null;
        }
        RegistryAuth copy = new RegistryAuth();
        copy.setRegistry(source.getRegistry());
        copy.setUsername(source.getUsername());
        return copy;
    }

}
