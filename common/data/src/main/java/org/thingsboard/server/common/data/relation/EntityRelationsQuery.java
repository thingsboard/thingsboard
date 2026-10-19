// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.relation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
@Schema
public class EntityRelationsQuery {

    @Schema(description = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @Schema(description = "Main filters.")
    private List<RelationEntityTypeFilter> filters;

}
