// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.service.DaoTimescaleTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Globally-applied Spring test hook that, ONLY when {@code -Dtb.citus.alltests=true} is set, repoints EVERY test
 * {@code ApplicationContext} in the run at the shared {@link CitusTestCluster} coordinator and enables Citus mode.
 * This lets {@code mvn test -Dtb.citus.alltests=true ...} rerun the whole {@code @DaoSqlTest}-based suite on Citus
 * in one shot, without writing a {@code @CitusDaoSqlTest} subclass per test class (a one-time regression sweep).
 * <p>
 * Discovered by Spring via {@code META-INF/spring.factories} under the
 * {@code org.springframework.test.context.ContextCustomizerFactory} key.
 * <p>
 * When the flag is absent, {@link #createContextCustomizer} returns {@code null}: contexts are unaffected AND the
 * context-cache key is unchanged, so plain (non-Citus) runs behave exactly as before. When the flag is set, every
 * factory invocation returns an equal customizer, so Spring's context cache still collapses identical configs.
 */
@Slf4j
public class CitusAllTestsContextCustomizerFactory implements ContextCustomizerFactory {

    static final String FLAG = "tb.citus.alltests";

    /** So the build log carries exactly one unmissable line proving the sweep flag actually reached this fork. */
    private static final AtomicBoolean FLAG_DETECTION_LOGGED = new AtomicBoolean();

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        if (!Boolean.getBoolean(FLAG)) {
            return null;
        }
        if (FLAG_DETECTION_LOGGED.compareAndSet(false, true)) {
            log.info("-D{}=true detected: repointing every test ApplicationContext at the shared Citus cluster", FLAG);
        }
        // Skip tests bound to an alternative backend datasource (TimescaleDB / Cassandra-NoSQL). Forcing the
        // Citus datasource onto them breaks backend-specific functionality (e.g. Timescale's time_bucket()),
        // so leave them on their own datasource.
        if (AnnotatedElementUtils.hasAnnotation(testClass, DaoTimescaleTest.class)
                || AnnotatedElementUtils.hasAnnotation(testClass, DaoNoSqlTest.class)) {
            log.info("Citus all-tests sweep: leaving {} on its own datasource (Timescale/NoSQL-bound)",
                    testClass.getName());
            return null;
        }
        return new CitusAllTestsContextCustomizer();
    }

    /**
     * Adds the shared Citus datasource property overrides via {@code addFirst} (same map as
     * {@link CitusDaoTestContextInitializer}). All instances are equal so the context cache keys identically.
     */
    @Slf4j
    static final class CitusAllTestsContextCustomizer implements ContextCustomizer {

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                                     MergedContextConfiguration mergedConfig) {
            log.debug("Citus all-tests sweep: repointing the context of {} at the Citus coordinator",
                    mergedConfig.getTestClass().getName());
            CitusTestCluster cluster = CitusTestCluster.getInstance();
            context.getEnvironment().getPropertySources()
                    .addFirst(new MapPropertySource(CitusDaoTestProperties.PROPERTY_SOURCE_NAME,
                            CitusDaoTestProperties.asMap(cluster)));
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && getClass() == obj.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}
