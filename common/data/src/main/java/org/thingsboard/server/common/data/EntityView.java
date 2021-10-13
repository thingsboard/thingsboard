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

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * Created by Victor Basanets on 8/27/2017.
 */

@ApiModel
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EntityView extends SearchTextBasedWithAdditionalInfo<EntityViewId>
        implements HasName, HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 5582010124562018986L;

    @ApiModelProperty(position = 8, value = "JSON object with Entity Id.", readOnly = true)
    private EntityId entityId;
    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    private String name;
    @ApiModelProperty(position = 9, required = true, value = "Type of entity view", example = "new-entity-view-type")
    @NoXss
    private String type;
    @ApiModelProperty(position = 10, required = true, value = "JSON object with parameters 'timeseries' and 'attributes'", readOnly = true)
    private TelemetryEntityView keys;
    @ApiModelProperty(position = 11, value = "Start timestamp of interval enable to see entity telemetry for the customer, in milliseconds", example = "1609459200000", readOnly = true)
    private long startTimeMs;
    @ApiModelProperty(position = 12, value = "End timestamp of interval enable to see entity telemetry for the customer, in milliseconds", example = "1609459200000", readOnly = true)
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
    @ApiModelProperty(position = 1, value = "JSON object with the entity view id. " +
            "Specify this field to update the entity view. " +
            "Referencing non-existing entity view Id will cause error. " +
            "Omit this field to create new entity view." )
    @Override
    public EntityViewId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the entity view creation, in milliseconds", example = "1609459200000", readOnly = true)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, required = true, value = "Unique Entity View Name in scope of Tenant", example = "New Entity View Name", readOnly = true)
    @Override
    public String getSearchText() {
        return getName() /*What the ...*/;
    }

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignEntityViewToCustomer' to change the Customer Id.", readOnly = true)
    @Override
    public CustomerId getCustomerId() {
        return customerId;
    }

    @ApiModelProperty(position = 5,  required = true, value = "Unique name of entity view in scope of Tenant", example = "New Entity View Name", readOnly = true)
    @Override
    public String getName() {
        return name;
    }

    @ApiModelProperty(position = 6, value = "JSON object with Tenant Id.", readOnly = true)
    @Override
    public TenantId getTenantId() {
        return tenantId;
    }

    @ApiModelProperty(position = 7, value = "JsonNode object with key-value parameters", example = "{\"description\":\"this is description\"}", readOnly = true)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }
}
