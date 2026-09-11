// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
