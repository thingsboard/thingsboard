// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;

import java.util.UUID;

public record RelationData(UUID fromId, EntityType fromType, UUID toId, EntityType toType, String type,
                           RelationTypeGroup typeGroup) {

}
