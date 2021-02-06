/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;

/**
 * Created by Victor Basanets on 8/27/2017.
 */

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EntityView extends SearchTextBasedWithAdditionalInfo<EntityViewId>
        implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 5582010124562018986L;

    private EntityId entityId;
    private TenantId tenantId;
    private CustomerId customerId;
    private String name;
    private String type;
    private TelemetryEntityView keys;
    private long startTimeMs;
    private long endTimeMs;

    public EntityView() {
        super();
    }

    public EntityView(EntityViewId id) {
        super(id);
    }

    public EntityView(EntityView entityView) {
        super(entityView);
        this.entityId = entityView.getEntityId();
        this.tenantId = entityView.getTenantId();
        this.customerId = entityView.getCustomerId();
        this.name = entityView.getName();
        this.type = entityView.getType();
        this.keys = entityView.getKeys();
        this.startTimeMs = entityView.getStartTimeMs();
        this.endTimeMs = entityView.getEndTimeMs();
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
}
