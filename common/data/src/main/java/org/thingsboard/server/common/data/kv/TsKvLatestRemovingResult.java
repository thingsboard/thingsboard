// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class TsKvLatestRemovingResult {
    private String key;
    private TsKvEntry data;
    private boolean removed;
    private Long version;

    public TsKvLatestRemovingResult(String key, boolean removed) {
        this(key, removed, null);
    }

    public TsKvLatestRemovingResult(TsKvEntry data, Long version) {
        this.key = data.getKey();
        this.data = data;
        this.removed = true;
        this.version = version;
    }

    public TsKvLatestRemovingResult(String key, boolean removed, Long version) {
        this.key = key;
        this.removed = removed;
        this.version = version;
    }
}
