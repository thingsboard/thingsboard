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
package org.thingsboard.server.edqs.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.edqs.data.dp.BoolDataPoint;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.edqs.data.dp.LongDataPoint;
import org.thingsboard.server.edqs.data.dp.StringDataPoint;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToString
public abstract class BaseEntityData<T extends EntityFields> implements EntityData<T> {

    @Getter
    private final UUID id;
    @Getter
    protected final Map<Integer, DataPoint> serverAttrMap;
    @Getter
    private final Map<Integer, DataPoint> tMap;

    @Getter
    @Setter
    private volatile UUID customerId;

    @Setter
    protected TenantRepo repo;

    @Getter
    @Setter
    protected volatile T fields;

    public BaseEntityData(UUID id) {
        this.id = id;
        this.serverAttrMap = new ConcurrentHashMap<>();
        this.tMap = new ConcurrentHashMap<>();
    }

    @Override
    public DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType) {
        return switch (entityKeyType) {
            case ATTRIBUTE, SERVER_ATTRIBUTE -> serverAttrMap.get(keyId);
            default -> null;
        };
    }

    @Override
    public boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value) {
        return serverAttrMap.put(keyId, value) == null;
    }

    @Override
    public boolean removeAttr(Integer keyId, AttributeScope scope) {
        return serverAttrMap.remove(keyId) != null;
    }

    @Override
    public DataPoint getTs(Integer keyId) {
        return tMap.get(keyId);
    }

    @Override
    public boolean putTs(Integer keyId, DataPoint value) {
        return tMap.put(keyId, value) == null;
    }

    @Override
    public boolean removeTs(Integer keyId) {
        return tMap.remove(keyId) != null;
    }

    @Override
    public String getOwnerName() {
        return repo.getOwnerEntityName(isTenantEntity() ? repo.getTenantId() : new CustomerId(getCustomerId()));
    }

    @Override
    public String getOwnerType() {
        return isTenantEntity() ? EntityType.TENANT.name() :  EntityType.CUSTOMER.name();
    }

    @Override
    public DataPoint getDataPoint(DataKey key, QueryContext ctx) {
        return switch (key.type()) {
            case TIME_SERIES -> getTs(key.keyId());
            case ATTRIBUTE, SERVER_ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE -> getAttr(key.keyId(), key.type());
            case ENTITY_FIELD -> getField(key, ctx);
            default -> throw new RuntimeException(key.type() + " not supported");
        };
    }

    private DataPoint getField(DataKey newKey, QueryContext ctx) {
        if (fields == null) {
            return null;
        }
        String key = newKey.key();
        return switch (key) {
            case "createdTime" -> new LongDataPoint(System.currentTimeMillis(), fields.getCreatedTime());
            case "edgeTemplate" -> new BoolDataPoint(System.currentTimeMillis(), fields.isEdgeTemplate());
            case "parentId" -> new StringDataPoint(System.currentTimeMillis(), getRelatedParentId(ctx), false);
            default -> new StringDataPoint(System.currentTimeMillis(), getField(key), false);
        };
    }

    @Override
    public String getField(String name) {
        if (fields == null) {
            return null;
        }
        return switch (name) {
            case "name" -> getEntityName();
            case "ownerName" -> getOwnerName();
            case "ownerType" -> getOwnerType();
            case "displayName" -> getDisplayName();
            case "entityType" -> Optional.ofNullable(getEntityType()).map(EntityType::name).orElse("");
            default -> fields.getAsString(name);
        };
    }

    public String getDisplayName(){
        return switch (getEntityType()) {
            case DEVICE, ASSET -> StringUtils.isNotBlank(fields.getLabel()) ? fields.getLabel() : fields.getName();
            case USER -> {
                boolean firstNameSet = StringUtils.isNotBlank(fields.getFirstName());
                boolean lastNameSet = StringUtils.isNotBlank(fields.getLastName());
                if(firstNameSet && lastNameSet) {
                    yield fields.getFirstName() + " " + fields.getLastName();
                } else if(firstNameSet) {
                    yield fields.getFirstName();
                } else if  (lastNameSet) {
                    yield fields.getLastName();
                } else {
                    yield fields.getEmail();
                }
            }
            default -> fields.getName();
        };
    }

    public String getEntityName() {
        return getFields().getName();
    }

    private boolean isTenantEntity() {
        return getCustomerId() == null || CustomerId.NULL_UUID.equals(getCustomerId());
    }

    private String getRelatedParentId(QueryContext ctx) {
        return Optional.ofNullable(ctx.getRelatedParentIdMap().get(getId()))
                .map(UUID::toString)
                .orElse("");
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return fields == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntityData<?> that = (BaseEntityData<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
