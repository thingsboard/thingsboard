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
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.Collections;
import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetRelatedDataNodeConfiguration extends TbGetEntityDataNodeConfiguration {

    private RelationsQuery relationsQuery;

    @Override
    public TbGetRelatedDataNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetRelatedDataNodeConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.putIfAbsent("serialNumber", "sn");
        configuration.setDataMapping(dataMapping);
        configuration.setDataToFetch(DataToFetch.ATTRIBUTES);
        configuration.setFetchTo(TbMsgSource.METADATA);

        var relationsQuery = new RelationsQuery();
        var relationEntityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        relationsQuery.setFilters(Collections.singletonList(relationEntityTypeFilter));
        configuration.setRelationsQuery(relationsQuery);

        return configuration;
    }

}
