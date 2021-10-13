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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityViewId;

@ApiModel
@Data
public class EntityViewInfo extends EntityView {

    @ApiModelProperty(position = 12, value = "Unique name of the customer that assigns to entity view. Use " +
            "'assignEntityViewToCustomer' to change the customer name.", example = "Customer A", readOnly = true)
    private String customerTitle;
    @ApiModelProperty(position = 14, value = "Boolean value representing is customer is public", example = "false", readOnly = true)
    private boolean customerIsPublic;

    public EntityViewInfo() {
        super();
    }

    public EntityViewInfo(EntityViewId entityViewId) {
        super(entityViewId);
    }

    public EntityViewInfo(EntityView entityView, String customerTitle, boolean customerIsPublic) {
        super(entityView);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}
