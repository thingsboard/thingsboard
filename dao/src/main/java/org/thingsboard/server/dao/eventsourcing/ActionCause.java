// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.eventsourcing;

public enum ActionCause {
    TENANT_DELETION,
    // The entity is being removed as part of a bulk operation whose related async cleanup (e.g. alarm comments) is
    // batched by the caller, so per-entity deletion listeners should skip their own per-entity follow-up tasks.
    BULK_DELETION
}
