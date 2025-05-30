/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edqs.query;

import lombok.Builder;
import org.thingsboard.server.common.data.query.EntityFilter;

import java.util.List;

public class EdqsCountQuery extends EdqsQuery {

    @Builder
    EdqsCountQuery(EntityFilter entityFilter, boolean hasKeyFilters, List<EdqsFilter> keyFilters) {
        super(entityFilter, hasKeyFilters, keyFilters);
    }

}
