// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema
public enum DataType {

    BOOLEAN(0),
    LONG(1),
    DOUBLE(2),
    STRING(3),
    JSON(4);

    @Getter
    private final int protoNumber; // Corresponds to KeyValueType

    DataType(int protoNumber) {
        this.protoNumber = protoNumber;
    }

}
