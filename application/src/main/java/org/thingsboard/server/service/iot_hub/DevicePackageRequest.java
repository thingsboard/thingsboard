// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.iot_hub;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The install or update of a device package as the install wizard planned it: the entity of each install step
 * of the version being installed, already resolved from its template, and what to do with it. The server applies
 * all of it in one transaction, together with the installed item, or nothing.
 */
@Data
public class DevicePackageRequest {

    private String versionId;
    // Update only: the version installed when the wizard planned the update, which fails if the package changed since
    private UUID fromVersionId;
    // The install method of the steps; an update without it keeps the installed one
    private String selectedInstallMethod;
    // The form values of the install state, by step: the server adds the entities to it
    private Map<String, JsonNode> installState;
    // Install only, from a customer's or a device group's page: the customer the device, the gateway and the dashboard
    // go to, and the group the created device or gateway is added to. An update ignores them
    private UUID customerId;
    private UUID entityGroupId;
    private List<Step> steps;

    public enum Action {
        // Taken as it is: an update's new version doesn't change it, or the user kept an entity they had
        KEEP,
        // Takes the entity from the step
        OVERWRITE,
        // A new entity: one the package installs, a step new in this version or deleted since install, or a copy
        // next to one with the same name
        CREATE
    }

    @Data
    public static class Step {
        private String name;
        // The package step's stable id, so that a next version can rename the step. It is also the step's install state
        // key when set, which the default converter update relies on to write the state back under the stored key
        private String id;
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
        // OVERWRITE and CREATE: the entity resolved from the template. ${<alias>.<property>} of an entity the
        // request creates is left for the server, which knows its id only once it is created.
        private JsonNode entity;
        // DEVICE and GATEWAY: the credentials template, if any
        private JsonNode credentials;
        // OVERWRITE and CREATE: the attributes of the saved entity, written after the transaction. Like the entity,
        // they may keep ${<alias>.<property>} of an entity the request creates.
        private JsonNode serverAttributes;
        private JsonNode sharedAttributes;
        // The resolution the install state records for it
        private String resolution;
        // INTEGRATION: the step's metadata entries, which the package's converter reads. A KEEP merges them into the
        // integration, e.g. one the package took as it was; null removes an entry
        private JsonNode metadata;
        // UPLINK_CONVERTER: the step takes ThingsBoard's default converter. The install state records it, so that a later
        // update checks that converter against ThingsBoard's default one
        private boolean defaultConverter;

        /**
         * The key of the step's install state: its id, or else its type and name, so that two steps of an install
         * method never share it. The install wizard keys the steps of an install the same way.
         */
        public String stateKey() {
            return id != null ? id : type + ":" + name;
        }
    }

}
