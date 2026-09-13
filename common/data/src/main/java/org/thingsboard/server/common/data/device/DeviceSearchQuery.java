// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;

import java.util.Collections;
import java.util.List;

@Schema
@Data
public class DeviceSearchQuery {

    @Schema(description = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @Schema(description = "Type of the relation between root entity and device (e.g. 'Contains' or 'Manages').")
    private String relationType;
    @Schema(description = "Array of device types to filter the related entities (e.g. 'Temperature Sensor', 'Smoke Sensor').")
    private List<String> deviceTypes;

    public EntityRelationsQuery toEntitySearchQuery() {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(parameters);
        query.setFilters(
                Collections.singletonList(new RelationEntityTypeFilter(relationType == null ? EntityRelation.CONTAINS_TYPE : relationType,
                        Collections.singletonList(EntityType.DEVICE))));
        return query;
    }
}
