// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.query;

public interface QueryLogComponent {

    void logQuery(SqlQueryContext ctx, String query, long duration);
}
