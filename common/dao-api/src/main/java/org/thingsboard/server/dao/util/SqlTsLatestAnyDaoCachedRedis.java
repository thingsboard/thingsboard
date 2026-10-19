// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("('${database.ts_latest.type}'=='sql' || '${database.ts_latest.type}'=='timescale') && '${cache.ts_latest.enabled:false}'=='true' && '${cache.type:caffeine}'=='redis' ")
public @interface SqlTsLatestAnyDaoCachedRedis {
}
