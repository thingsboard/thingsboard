// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

import java.util.UUID;

public record TraceGroupKey(UUID tenantId, String pathId) {
}
