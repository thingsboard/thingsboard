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
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResource extends TbResourceInfo {

    private static final long serialVersionUID = 7379609705527272306L;

    private String data;

    public TbResource() {
        super();
    }

    public TbResource(TbResourceId id) {
        super(id);
    }

    public TbResource(TbResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    public TbResource(TbResource resource) {
        super(resource);
        this.data = resource.getData();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Resource [tenantId=");
        builder.append(getTenantId());
        builder.append(", id=");
        builder.append(getUuidId());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", resourceType=");
        builder.append(getResourceType());
        builder.append(", resourceKey=");
        builder.append(getResourceKey());
        builder.append(", data=");
        builder.append(data);
        builder.append("]");
        return builder.toString();
    }
}
