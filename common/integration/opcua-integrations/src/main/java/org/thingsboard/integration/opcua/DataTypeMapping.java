// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.kv.DataType;

@Data
@AllArgsConstructor
public class DataTypeMapping {

    private DataType dataType;

    @JsonCreator
    public static DataTypeMapping forValue(String value) {
        return new DataTypeMapping(DataType.valueOf(value.toUpperCase()));
    }

}