// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-integration-executor' || '${service.type:null}'=='tb-integration'")
public @interface TbIntegrationExecutorOrIntegrationComponent {}
