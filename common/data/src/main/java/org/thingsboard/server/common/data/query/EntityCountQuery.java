// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Schema
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityCountQuery {

    @Getter
    private EntityFilter entityFilter;

    @Getter
    protected List<KeyFilter> keyFilters;

    public EntityCountQuery() {
    }

    public EntityCountQuery(EntityFilter entityFilter) {
        this(entityFilter, Collections.emptyList());
    }

    public EntityCountQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        this.entityFilter = entityFilter;
        this.keyFilters = keyFilters;
    }
}
