// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.timeseries;

import java.util.Optional;

public enum TsInsertExecutorType {
    SINGLE,
    FIXED,
    CACHED;

    public static Optional<TsInsertExecutorType> parse(String name) {
        TsInsertExecutorType executorType = null;
        if (name != null) {
            for (TsInsertExecutorType type : TsInsertExecutorType.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    executorType = type;
                    break;
                }
            }
        }
        return Optional.of(executorType);
    }
}
