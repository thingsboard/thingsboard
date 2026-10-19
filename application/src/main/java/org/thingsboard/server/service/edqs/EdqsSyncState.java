// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edqs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsState.EdqsSyncStatus;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EdqsSyncState {
    private EdqsSyncStatus status;
    private Set<ObjectType> objectTypes;
}
