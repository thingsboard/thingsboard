// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.server.dao.sql.citus.CitusDaoTestContextInitializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reruns an existing {@code @DaoSqlTest} service test against a real Citus cluster.
 * <p>
 * Subclass an existing {@code @DaoSqlTest} service test and annotate the subclass with this annotation, e.g.:
 * <pre>{@code
 * @CitusDaoSqlTest
 * public class CitusAlarmServiceTest extends AlarmServiceTest {}
 * }</pre>
 * Every inherited {@code @Test} then reruns against the Citus coordinator+worker cluster managed by
 * {@link CitusDaoTestContextInitializer}/{@code CitusTestCluster}, with the real ThingsBoard schema installed
 * and the real {@code DefaultCitusSchemaService.applyDistribution()} applied.
 * <p>
 * It is meta-annotated with {@link DaoSqlTest} (so the {@code @TestPropertySource} location list lives only there)
 * plus a {@link ContextConfiguration#initializers() context initializer} that, via an {@code addFirst} property
 * source, repoints the datasource at the Citus coordinator (plain {@code org.postgresql.Driver}) and enables Citus
 * mode.
 * Because the initializer and properties differ from the plain {@code @DaoSqlTest} setup, Spring caches this as a
 * distinct application context, so the plain and Citus variants of a test can both run in the same suite without
 * clobbering each other's context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@DaoSqlTest
@ContextConfiguration(initializers = CitusDaoTestContextInitializer.class)
public @interface CitusDaoSqlTest {
}
