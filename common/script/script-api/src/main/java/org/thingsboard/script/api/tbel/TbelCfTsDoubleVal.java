// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import lombok.Data;

@Data
public class TbelCfTsDoubleVal implements TbelCfObject {

    public static final long OBJ_SIZE = 32L; // Approximate calculation;

    private final long ts;
    private final double value;

    @Override
    public long memorySize() {
        return OBJ_SIZE;
    }
}
