// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class TsKvEntryAggWrapper {

    private final TsKvEntry entry;
    private final long lastEntryTs;

}
