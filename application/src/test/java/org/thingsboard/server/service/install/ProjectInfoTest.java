// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.info.BuildProperties;

import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectInfoTest {

    @ParameterizedTest
    @CsvSource({
            "4.2.2, 4.2.2",
            "4.2.2-SNAPSHOT, 4.2.2",
            "4.2.2PE-SNAPSHOT, 4.2.2",
            "4.2.2.8PE, 4.2.2.8",
            "4.2.2PAAS.cbeee8ba6f2, 4.2.2",
            "4.2.2-RC1, 4.2.2"
    })
    void keepsOnlyTheLeadingNumericVersion(String buildVersion, String expected) {
        Properties properties = new Properties();
        properties.setProperty("version", buildVersion);

        assertThat(new ProjectInfo(Optional.of(new BuildProperties(properties))).getProjectVersion()).isEqualTo(expected);
    }

}
