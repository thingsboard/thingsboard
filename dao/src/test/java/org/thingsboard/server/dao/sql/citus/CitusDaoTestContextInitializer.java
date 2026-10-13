// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring {@link ApplicationContextInitializer} that points the test datasource at the shared
 * {@link CitusTestCluster} coordinator (over the plain {@code org.postgresql.Driver}) and enables Citus mode.
 * <p>
 * Tests here are JUnit4 {@code @RunWith(SpringRunner.class)}, so a context initializer — not a JUnit5
 * {@code @DynamicPropertySource} — is the correct hook to inject the dynamically-allocated container port.
 * The datasource properties are added via {@code addFirst} as a highest-precedence property source, so they
 * override the {@code spring.datasource.*} values declared in the {@code @TestPropertySource} files
 * (notably the Testcontainers {@code jdbc:tc:postgresql} URL in {@code sql-test.properties}).
 */
public class CitusDaoTestContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        CitusTestCluster cluster = CitusTestCluster.getInstance();
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource(CitusDaoTestProperties.PROPERTY_SOURCE_NAME,
                        CitusDaoTestProperties.asMap(cluster)));
    }
}
