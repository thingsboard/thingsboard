// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import org.thingsboard.server.common.data.EntityType;

public interface EntityDefinition {

    String getName();

    EntityType getEntityType();

}
