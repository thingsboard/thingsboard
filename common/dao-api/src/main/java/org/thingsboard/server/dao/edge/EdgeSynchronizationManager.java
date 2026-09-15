// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import org.thingsboard.server.common.data.id.EdgeId;

public interface EdgeSynchronizationManager {

    ThreadLocal<EdgeId> getEdgeId();
}
