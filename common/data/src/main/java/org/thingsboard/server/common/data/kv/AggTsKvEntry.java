// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.ToString;
import org.thingsboard.server.common.data.query.TsValue;

@ToString
public class AggTsKvEntry extends BasicTsKvEntry {

    private static final long serialVersionUID = -1933884317450255935L;

    private final long count;

    public AggTsKvEntry(long ts, KvEntry kv, long count) {
        super(ts, kv);
        this.count = count;
    }

    @Override
    public TsValue toTsValue() {
        return new TsValue(ts, getValueAsString(), count);
    }
}
