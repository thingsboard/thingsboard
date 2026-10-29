// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The update of an installed device package as the install wizard planned it: the entity of each install step
 * of the new version, already resolved from its template, and what to do with it. The server applies all of it
 * in one transaction, together with the new version of the installed item, or nothing.
 */
@Data
public class DeviceUpdateRequest {

    private String versionId;
    // The install method of the new version's steps; the installed one when not set
    private String selectedInstallMethod;
    // The form values of the install state, by step: the server adds the entities to it
    private Map<String, JsonNode> installState;
    private List<Step> steps;

    public enum Action {
        // Taken as it is: the new version doesn't change it, or the user kept an entity they had
        KEEP,
        // Takes the entity from the step
        OVERWRITE,
        // A new entity: new in this version, deleted since install, or a copy next to one with the same name
        CREATE
    }

    @Data
    public static class Step {
        private String name;
        // InstallStepType of the package: DEVICE_PROFILE, DEVICE, GATEWAY, GATEWAY_CONNECTOR, DASHBOARD, RULE_CHAIN,
        // UPLINK_CONVERTER, DOWNLINK_CONVERTER, INTEGRATION
        private String type;
        private Action action;
        // KEEP and OVERWRITE: the entity
        private UUID entityId;
        // OVERWRITE: the version the user reviewed; a different one means someone changed the entity since
        private Long expectedVersion;
        // The user confirmed overwriting an entity of another package or one they didn't install with this package
        private boolean confirmed;
        // OVERWRITE and CREATE: the entity resolved from the template. ${<alias>.<property>} of an entity this
        // update creates is left for the server, which knows its id only once it is created.
        private JsonNode entity;
        // DEVICE and GATEWAY: the credentials template, if any
        private JsonNode credentials;
        // DEVICE and GATEWAY created by this update: attributes, saved after the transaction
        private JsonNode serverAttributes;
        private JsonNode sharedAttributes;
        // The resolution the install state records for it
        private String resolution;
    }

}
