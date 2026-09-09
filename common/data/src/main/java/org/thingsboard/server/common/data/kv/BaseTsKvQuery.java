// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class BaseTsKvQuery implements TsKvQuery {

    private static final ThreadLocal<Integer> idSeq = ThreadLocal.withInitial(() -> 0);

    private final int id;
    private final String key;
    private final long startTs;
    private final long endTs;

    public BaseTsKvQuery(String key, long startTs, long endTs) {
        this(idSeq.get(), key, startTs, endTs);
        idSeq.set(id + 1);
    }

    protected BaseTsKvQuery(int id, String key, long startTs, long endTs) {
        this.id = id;
        this.key = key;
        this.startTs = startTs;
        this.endTs = endTs;
    }

}
