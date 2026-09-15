// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.relation;

import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

public record EntityRelationPathQuery(EntityId rootEntityId, List<RelationPathLevel> levels) {

}
