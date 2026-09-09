// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class TbelCfTsMultiDoubleVal implements TbelCfObject {

    public static final long OBJ_SIZE = 32L; // Approximate calculation;

    private final long ts;
    private final double[] values;

    @JsonIgnore
    public double getV1() {
        return getV(0);
    }

    @JsonIgnore
    public double getV2() {
        return getV(1);
    }

    @JsonIgnore
    public double getV3() {
        return getV(2);
    }

    @JsonIgnore
    public double getV4() {
        return getV(3);
    }

    @JsonIgnore
    public double getV5() {
        return getV(4);
    }

    private double getV(int idx) {
        if (values.length < idx + 1) {
            throw new IllegalArgumentException("Can't get value at index " + idx + ". There are " + values.length + " values present.");
        } else {
            return values[idx];
        }
    }

    @Override
    public long memorySize() {
        return OBJ_SIZE + values.length * 8L;
    }
}
