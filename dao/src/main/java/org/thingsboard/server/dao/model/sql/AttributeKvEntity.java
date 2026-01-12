/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.model.ToData;

import java.io.Serializable;

import static org.thingsboard.server.dao.model.ModelConstants.BOOLEAN_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.DOUBLE_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.JSON_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LAST_UPDATE_TS_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LONG_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.STRING_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

@Data
@Entity
@Table(name = "attribute_kv")
public class AttributeKvEntity implements ToData<AttributeKvEntry>, Serializable {

    @EmbeddedId
    private AttributeKvCompositeKey id;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    private String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    private Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    @Column(name = JSON_VALUE_COLUMN)
    private String jsonValue;

    @Column(name = LAST_UPDATE_TS_COLUMN)
    private Long lastUpdateTs;

    @Column(name = VERSION_COLUMN)
    private Long version;

    @Transient
    protected String strKey;

    @Override
    public AttributeKvEntry toData() {
        KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(strKey, strValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(strKey, booleanValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(strKey, doubleValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(strKey, longValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(strKey, jsonValue);
        }

        return new BaseAttributeKvEntry(kvEntry, lastUpdateTs, version);
    }
}
