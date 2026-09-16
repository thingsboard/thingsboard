// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.data;

import lombok.Data;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;

import java.util.List;

@Data
public class RelationsQuery {

    private EntitySearchDirection direction;
    private int maxLevel = 1;
    private List<RelationEntityTypeFilter> filters;
    private boolean fetchLastLevelOnly = false;
}
