// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProjectInfo {

    private final Optional<BuildProperties> buildProperties;

    public String getProjectVersion() {
        return buildProperties.orElseThrow(() -> new IllegalStateException("Build properties are missing. Please rebuild the project with maven"))
                .getVersion().replaceAll("[^\\d.]", "");
    }

    public String getProductType() {
        return "CE";
    }

}
