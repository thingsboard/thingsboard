/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.*;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
@EqualsAndHashCode
@ToString
public final class TsKvLatestEntity implements ToData<TsKvEntry> {

    @Id
    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN)
    private UUID entityId;

    @Id
    @Column(name = KEY_COLUMN)
    private String key;

    @Column(name = TS_COLUMN)
    private long ts;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    private String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    private Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    @Override
    public TsKvEntry toData() {
        return null;
    }
}
