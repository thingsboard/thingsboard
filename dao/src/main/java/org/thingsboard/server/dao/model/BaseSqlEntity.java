/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 13.07.17.
 */
@Data
@MappedSuperclass
public abstract class BaseSqlEntity<D> implements BaseEntity<D> {

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    @Column(name = ModelConstants.CREATED_TIME_PROPERTY, updatable = false)
    protected long createdTime;

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        if (createdTime > 0) {
            this.createdTime = createdTime;
        }
    }

    protected static UUID getUuid(UUIDBased uuidBased) {
        if (uuidBased != null) {
            return uuidBased.getId();
        } else {
            return null;
        }
    }

    protected static UUID getTenantUuid(TenantId tenantId) {
        if (tenantId != null && !tenantId.isNullUid()) {
            return tenantId.getId();
        } else {
            return null;
        }
    }

    protected static <I> I getEntityId(UUID uuid, Function<UUID, I> creator) {
        if (uuid != null) {
            return creator.apply(uuid);
        } else {
            return null;
        }
    }

    protected static TenantId getTenantId(UUID uuid) {
        if (uuid != null && !uuid.equals(EntityId.NULL_UUID)) {
            return TenantId.fromUUID(uuid);
        } else {
            return TenantId.SYS_TENANT_ID;
        }
    }

    protected JsonNode toJson(Object value) {
        if (value != null) {
            return JacksonUtil.valueToTree(value);
        } else {
            return null;
        }
    }

    protected <T> T fromJson(JsonNode json, Class<T> type) {
        return JacksonUtil.convertValue(json, type);
    }

    protected String listToString(List<?> list) {
        if (list != null) {
            return StringUtils.join(list, ',');
        } else {
            return "";
        }
    }

    protected <E> List<E> listFromString(String string, Function<String, E> mappingFunction) {
        if (string != null) {
            return Arrays.stream(StringUtils.split(string, ','))
                    .filter(StringUtils::isNotBlank)
                    .map(mappingFunction).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}
