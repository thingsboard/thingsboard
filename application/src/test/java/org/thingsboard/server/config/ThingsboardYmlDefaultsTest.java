// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThingsboardYmlDefaultsTest {

    @Test
    void tomcatMaxParameterCountDefaultsTo10000() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("thingsboard", new ClassPathResource("thingsboard.yml"));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(environment.getPropertySources()::addLast);
        assertThat(environment.getProperty("server.tomcat.max-parameter-count", Integer.class)).isEqualTo(10000);
    }

}
