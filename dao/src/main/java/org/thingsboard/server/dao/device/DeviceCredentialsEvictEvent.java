// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * There is deliberately no (newId, oldId) convenience constructor: every call site must state whether the
 * credentials ceased to exist, because a delete that defaults to {@code deleted = false} would silently downgrade
 * the hooks' "revoked" marker to a tombstone and let an in-flight validation re-cache the dead token.
 */
@Data
@AllArgsConstructor
public class DeviceCredentialsEvictEvent {

    private final String newCredentialsId;
    private final String oldCredentialsId;
    private final boolean deleted;

}
