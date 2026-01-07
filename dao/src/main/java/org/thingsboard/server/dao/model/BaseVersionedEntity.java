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
package org.thingsboard.server.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasVersion;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class BaseVersionedEntity<D extends BaseData & HasVersion> extends BaseSqlEntity<D> implements HasVersion {

    @Getter @Setter
    @Version
    @Column(name = ModelConstants.VERSION_PROPERTY)
    protected Long version;

    public BaseVersionedEntity() {
        super();
    }

    public BaseVersionedEntity(D domain) {
        super(domain);
        this.version = domain.getVersion();
    }

    public BaseVersionedEntity(BaseVersionedEntity<?> entity) {
        super(entity);
        this.version = entity.version;
    }

    @Override
    public String toString() {
        return "BaseVersionedEntity{" +
                "id=" + id +
                ", createdTime=" + createdTime +
                ", version=" + version +
                '}';
    }

}
