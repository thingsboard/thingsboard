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
package org.thingsboard.server.dao.entityview;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasVersion;

import java.io.Serializable;
import java.util.List;

@Getter
@EqualsAndHashCode
@Builder
public class EntityViewCacheValue implements Serializable, HasVersion {

    private static final long serialVersionUID = 1959004642076413174L;

    private final EntityView entityView;
    private final List<EntityView> entityViews;

    @Override
    public Long getVersion() {
        return entityView != null ? entityView.getVersion() : 0;
    }

}
