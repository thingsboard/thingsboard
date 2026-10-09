// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.coapserver;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("('${service.type:null}'=='tb-core' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-integration-executor') " +
        "&& '${coap.server.enabled:false}'=='true' || '${service.type:null}'=='tb-transport' || '${service.type:null}'=='tb-integration'")
public @interface TbCoapServerComponent {}
