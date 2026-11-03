// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TbTimeWindow implements TbelCfObject {

    public static final long OBJ_SIZE = 32L;

    private long startTs;
    private long endTs;

    @Override
    public long memorySize() {
        return OBJ_SIZE;
    }

    public boolean matches(long ts) {
        return ts >= startTs && ts < endTs;
    }
}
