// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data.dp;

import org.thingsboard.server.common.data.kv.DataType;

import java.util.function.Function;

public class CompressedJsonDataPoint extends CompressedStringDataPoint {

    public CompressedJsonDataPoint(long ts, byte[] compressedValue, Function<byte[], String> uncompressor) {
        super(ts, compressedValue, uncompressor);
    }

    @Override
    public DataType getType() {
        return DataType.JSON;
    }

}
