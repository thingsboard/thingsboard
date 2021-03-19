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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Slf4j
@Data
public class Resource implements HasTenantId, Serializable {

    private static final long serialVersionUID = 7379609705527272306L;

    private TenantId tenantId;
    private ResourceType resourceType;
    private String resourceId;
    private String textSearch;
    private String value;

    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder("Resource{");
        res.append("tenantId=").append(tenantId);
        res.append(", resourceType='").append(resourceType).append('\'');
        res.append(", resourceId='").append(resourceId).append('\'');
        res.append(", textSearch='").append(textSearch).append('\'');
        res.append('}');
        return res.toString();
    }
}
