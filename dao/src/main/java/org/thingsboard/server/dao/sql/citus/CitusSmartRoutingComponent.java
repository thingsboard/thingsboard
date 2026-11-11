// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composed gating for the Citus smart-routing beans: a bean is active only outside the {@code install}
 * profile AND when {@code database.citus.smart_routing.enabled=true}. Folding both conditions into a single
 * annotation keeps the gating uniform across every smart-routing bean so a new one cannot forget either
 * guard -- a bean that omitted {@code @Profile("!install")} would wrongly activate during install.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Profile("!install")
@ConditionalOnProperty(prefix = "database.citus.smart_routing", name = "enabled", havingValue = "true")
public @interface CitusSmartRoutingComponent {
}
