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
package org.thingsboard.server.common.data.dashboard.filter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

@DashboardStatefulRoot
@Data
public class DashboardRelationsQueryFilter implements DashboardStatefulRootFilter {

    private AliasEntityId rootEntity;

    private boolean rootStateEntity;

    private String stateEntityParamName;

    private AliasEntityId defaultStateEntity;

    @NotNull
    private EntitySearchDirection direction;

    @PositiveOrZero
    private int maxLevel;

    private boolean fetchLastLevelOnly;

    @Valid
    private List<RelationEntityTypeFilter> filters;

}
