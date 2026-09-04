// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.migrate;

public enum CassandraToSqlColumnType {
    ID,
    DOUBLE,
    INTEGER,
    FLOAT,
    BIGINT,
    BOOLEAN,
    STRING,
    JSON,
    ENUM_TO_INT
}
