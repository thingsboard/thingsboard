// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema
public class ExtensionContainerSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "Image repository without tag, including registry/org.",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "registry.example.com/acme/billing-ext")
    private String image;

    @NotBlank
    @Schema(description = "Image tag.", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.4.0")
    private String tag;

    @Valid
    @Schema(description = "Optional private-registry credentials for pulling the image. The password is a " +
            "ThingsBoard Secret reference, resolved at deploy time; the plaintext is never stored here. " +
            "Omit for public images.")
    private RegistryAuth registryAuth;

    @Min(1)
    @Max(20)
    @Schema(description = "Desired replica count. NOTE: honoured on the Kubernetes substrates only. In " +
            "docker-compose mode a single container runs behind one DNS name, so any value above 1 is " +
            "accepted and echoed back but exactly one container is started (and quota is charged for one).",
            defaultValue = "1", example = "2")
    private int replicas = 1;

    @Min(1)
    @Max(65535)
    @Schema(description = "Container HTTP port.", defaultValue = "8090", example = "8090")
    private int port = 8090;

    // May carry secrets. Deliberately returned on the TENANT_ADMIN-only detail read: an update
    // replaces containerSpec wholly, so a GET-modify-PUT edit needs the current env. Everywhere
    // less controlled it is hidden: stripped from the list projection (ExtensionInfo), scrubbed
    // from audit logs (AuditLogServiceImpl), and excluded from toString().
    @ToString.Exclude
    @Schema(description = "User-supplied environment. Keys starting with TB_EXT_ are reserved and rejected. " +
            "Values may be ThingsBoard Secret references (${secret:name;type:TEXT}), resolved at deploy time. " +
            "Returned only on the detail read (so updates can round-trip the spec); " +
            "omitted from list responses and audit logs.")
    private Map<String, String> env;

    @Valid
    @Schema(description = "Container resource requests/limits.")
    private ResourceSpec resources;

}
