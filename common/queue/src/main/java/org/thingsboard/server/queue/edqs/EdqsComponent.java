// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.edqs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${queue.edqs.sync.enabled:true}'=='true' && ('${service.type:null}'=='edqs' || " +
        "(('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && " +
        "'${queue.edqs.mode:null}'=='local'))")
public @interface EdqsComponent {}
