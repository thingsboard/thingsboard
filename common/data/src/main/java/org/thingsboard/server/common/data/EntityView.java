/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;

/**
 * Created by Victor Basanets on 8/27/2017.
 */
@EqualsAndHashCode(callSuper = true)
public class EntityView extends SearchTextBasedWithAdditionalInfo<EntityViewId>
        implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 5582010124562018986L;

    private EntityId entityId;
    private TenantId tenantId;
    private CustomerId customerId;
    private String name;
    private List<String> keys;
    private Long tsStart;
    private Long tsEnd;

    public EntityView() {
        super();
    }

    public EntityView(EntityViewId id) {
        super(id);
    }

    public EntityView(EntityId entityId,
                      TenantId tenantId,
                      CustomerId customerId,
                      String name,
                      List<String> keys,
                      Long tsStart,
                      Long tsEnd) {

        this.entityId = entityId;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.name = name;
        this.keys = keys;
        this.tsStart = tsStart;
        this.tsEnd = tsEnd;
    }

    public EntityView(EntityView entityView) {
        super(entityView);
    }

    public EntityId getEntityId() {
        return entityId;
    }

    public void setEntityId(EntityId entityId) {
        this.entityId = entityId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }

    public Long getTsStart() {
        return tsStart;
    }

    public void setTsStart(Long tsStart) {
        this.tsStart = tsStart;
    }

    public Long getTsEnd() {
        return tsEnd;
    }

    public void setTsEnd(Long tsEnd) {
        this.tsEnd = tsEnd;
    }

    @Override
    public String getSearchText() {
        return getName() /*What the ...*/;
    }

    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public String toString() {
        return "EntityView{entityId=" + entityId.getId() +
                ", tenantId=" + tenantId +
                ", customerId=" + customerId +
                ", name='" + name + "\'" +
                ", keys=" + String.join(",", keys) +
                ", tsStart=" + tsStart +
                ", tsEnd=" + tsEnd + "}";
    }
}
