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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

public interface ExportableEntity<I extends EntityId> extends HasId<I>, HasName {

    void setId(I id);

    @ApiModelProperty(position = 100, value = "JSON object with External Id from the VCS", accessMode = ApiModelProperty.AccessMode.READ_ONLY, hidden = true)
    I getExternalId();

    void setExternalId(I externalId);

    long getCreatedTime();

    void setCreatedTime(long createdTime);

    void setTenantId(TenantId tenantId);

}
