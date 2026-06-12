/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thingsboard.server.dao.model.sqlts.iotdb;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;


@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public final class IoTDBTsKvEntity extends AbstractTsKvEntity {
    private  String device;
    private  long timestamp;
    private  String measurement;
    private  Object value;
    private  TSDataType tsDataType;

    public IoTDBTsKvEntity() {
    }


    @Override
    public boolean isNotEmpty() {
        return ts != null && (strValue != null || longValue != null || doubleValue != null || booleanValue != null);
    }
}
