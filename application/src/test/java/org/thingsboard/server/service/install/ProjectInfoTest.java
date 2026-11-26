// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.info.BuildProperties;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectInfoTest {

    @ParameterizedTest
    @CsvSource({
            "4.4.1, 4.4.1",
            "4.4.1-SNAPSHOT, 4.4.1",
            "4.4.1PE-SNAPSHOT, 4.4.1",
            "4.4.1.2PE, 4.4.1.2",
            "4.4.1PAAS.cbeee8ba6f2, 4.4.1",
            "4.4.1-RC1, 4.4.1"
    })
    void keepsOnlyTheLeadingNumericVersion(String buildVersion, String expected) {
        Properties properties = new Properties();
        properties.setProperty("version", buildVersion);

        assertThat(new ProjectInfo(Optional.of(new BuildProperties(properties))).getProjectVersion()).isEqualTo(expected);
    }

    @Test
    void reportsUnknownWithoutBuildProperties() {
        assertThat(new ProjectInfo(Optional.empty()).getProjectVersion()).isEqualTo("unknown");
    }

}
