// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationDefinition {

    private EntityType entityType;
    private String entityName;
    private EntitySearchDirection direction;
    private String type;

}
