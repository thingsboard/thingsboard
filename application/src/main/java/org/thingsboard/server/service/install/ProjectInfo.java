// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class ProjectInfo {

    private final String version;

    public ProjectInfo(Optional<BuildProperties> buildProperties) {
        this.version = buildProperties
                .map(BuildProperties::getVersion)
                .map(v -> v.replaceAll("[^\\d.]", ""))
                .orElse(null);
    }

    public String getProjectVersion() {
        if (version == null) {
            log.warn("Cannot determine project version because build properties are missing. Please rebuild the project with maven");
            return "unknown";
        }
        return version;
    }

    // Hardcoded on purpose, and must stay "PE": this is the marker written to tb_schema_settings.product, and
    // every already-deployed database carries it. Changing it makes DefaultDatabaseSchemaSettingsService reject
    // every existing database as an unrecognized product, breaking all upgrades.
    public String getProductType() {
        return "PE";
    }

}
