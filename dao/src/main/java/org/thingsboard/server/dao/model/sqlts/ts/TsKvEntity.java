/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sqlts.ts;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ToData;
import org.thingsboard.server.dao.model.sql.AbsractTsKvEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;

@Data
@Entity
@Table(name = "ts_kv")
@IdClass(TsKvCompositeKey.class)
public final class TsKvEntity extends AbsractTsKvEntity implements ToData<TsKvEntry> {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_COLUMN)
    private EntityType entityType;

    public TsKvEntity() {
    }

    public TsKvEntity(String strValue) {
        this.strValue = strValue;
    }

    public TsKvEntity(Long longValue, Double doubleValue, Long longCountValue, Long doubleCountValue, String aggType) {
        if (!isAllNull(longValue, doubleValue, longCountValue, doubleCountValue)) {
            switch (aggType) {
                case AVG:
                    double sum = 0.0;
                    if (longValue != null) {
                        sum += longValue;
                    }
                    if (doubleValue != null) {
                        sum += doubleValue;
                    }
                    long totalCount = longCountValue + doubleCountValue;
                    if (totalCount > 0) {
                        this.doubleValue = sum / (longCountValue + doubleCountValue);
                    } else {
                        this.doubleValue = 0.0;
                    }
                    break;
                case SUM:
                    if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue + (longValue != null ? longValue.doubleValue() : 0.0);
                    } else {
                        this.longValue = longValue;
                    }
                    break;
                case MIN:
                case MAX:
                    if (longCountValue > 0 && doubleCountValue > 0) {
                        this.doubleValue = MAX.equals(aggType) ? Math.max(doubleValue, longValue.doubleValue()) : Math.min(doubleValue, longValue.doubleValue());
                    } else if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue;
                    } else if (longCountValue > 0) {
                        this.longValue = longValue;
                    }
                    break;
            }
        }
    }

    public TsKvEntity(Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount) {
        if (!isAllNull(booleanValueCount, strValueCount, longValueCount, doubleValueCount)) {
            if (booleanValueCount != 0) {
                this.longValue = booleanValueCount;
            } else if (strValueCount != 0) {
                this.longValue = strValueCount;
            } else {
                this.longValue = longValueCount + doubleValueCount;
            }
        }
    }


    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null;
    }
}
