// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Schema
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ExtensionInfo extends Extension {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Data-plane URL of the extension, e.g. /api/extension/route/acme-billing-9f3a.", accessMode = Schema.AccessMode.READ_ONLY)
    private String url;

    @Schema(description = "Number of ready replicas from the latest status snapshot; null when unknown.", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer readyReplicas;

    @Schema(description = "Desired replica count from the latest status snapshot; null when unknown.", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer desiredReplicas;

    public ExtensionInfo() {
        super();
    }

    public ExtensionInfo(Extension extension) {
        super(extension);
        ExtensionContainerSpec spec = extension.getContainerSpec();
        if (spec != null) {
            ExtensionContainerSpec light = new ExtensionContainerSpec();
            light.setImage(spec.getImage());
            light.setTag(spec.getTag());
            // keep the registry/username hint in the list view, but never the password (a secret ref)
            light.setRegistryAuth(RegistryAuth.redacted(spec.getRegistryAuth()));
            light.setReplicas(spec.getReplicas());
            light.setPort(spec.getPort());
            setContainerSpec(light);
        }
    }

}
