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
package org.thingsboard.server.dao.model.sqlts.latest;

import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;
import lombok.Data;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.VERSION_COLUMN;

@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "tsKvLatestFindMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TsKvLatestEntity.class,
                                columns = {
                                        @ColumnResult(name = "entityId", type = UUID.class),
                                        @ColumnResult(name = "key", type = Integer.class),
                                        @ColumnResult(name = "strKey", type = String.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "boolValue", type = Boolean.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "jsonValue", type = String.class),
                                        @ColumnResult(name = "ts", type = Long.class),
                                        @ColumnResult(name = "version", type = Long.class)
                                }
                        ),
                })
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID,
                query = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID_QUERY,
                resultSetMapping = "tsKvLatestFindMapping",
                resultClass = TsKvLatestEntity.class
        )
})
public final class TsKvLatestEntity extends AbstractTsKvEntity {

    @Column(name = VERSION_COLUMN)
    private Long version;

    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null;
    }

    public TsKvLatestEntity() {
    }

    public TsKvLatestEntity(UUID entityId, Integer key, String strKey, String strValue, Boolean boolValue, Long longValue, Double doubleValue, String jsonValue, Long ts, Long version) {
        this.entityId = entityId;
        this.key = key;
        this.ts = ts;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
        this.strValue = strValue;
        this.booleanValue = boolValue;
        this.jsonValue = jsonValue;
        this.strKey = strKey;
        this.version = version;
    }

    @Override
    public TsKvEntry toData() {
        TsKvEntry tsKvEntry = super.toData();
        tsKvEntry.setVersion(version);
        return tsKvEntry;
    }

}
