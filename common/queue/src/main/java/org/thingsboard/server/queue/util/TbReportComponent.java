// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.queue.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${service.type:null}' == 'tb-report' || ('${queue.report.mode:local}' == 'local' && " +
                         "('${service.type:null}' == 'monolith' || '${service.type:null}' == 'tb-core'))")
public @interface TbReportComponent {
}
