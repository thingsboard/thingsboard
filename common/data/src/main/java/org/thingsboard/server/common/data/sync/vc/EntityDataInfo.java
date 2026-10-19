// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntityDataInfo {
    boolean hasRelations;
    boolean hasAttributes;
    boolean hasCredentials;
    boolean hasCalculatedFields;
    boolean hasPermissions;
    boolean hasGroupEntities;
}
