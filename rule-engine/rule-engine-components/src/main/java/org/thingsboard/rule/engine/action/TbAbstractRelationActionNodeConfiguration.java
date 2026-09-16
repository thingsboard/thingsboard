// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
public abstract class TbAbstractRelationActionNodeConfiguration {

    private EntitySearchDirection direction;
    private String relationType;

    private EntityType entityType;
    private String entityNamePattern;
    private String entityTypePattern;

}
