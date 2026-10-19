// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("('${database.ts.type}'=='cassandra' || '${database.ts_latest.type}'=='cassandra') " +
        "&& ('${cassandra.cloud.secure_connect_bundle_path}' == null || '${cassandra.cloud.secure_connect_bundle_path}'.isBlank() )")
public @interface NoSqlAnyDaoNonCloud {
}
