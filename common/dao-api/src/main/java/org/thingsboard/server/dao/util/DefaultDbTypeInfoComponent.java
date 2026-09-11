// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultDbTypeInfoComponent implements DbTypeInfoComponent {

    @Value("${database.ts_latest.type:sql}")
    @Getter
    private String latestTsDbType;

    @Override
    public boolean isLatestTsDaoStoredToSql() {
        return !latestTsDbType.equalsIgnoreCase("cassandra");
    }
}
