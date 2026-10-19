// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EdgeId;

@Slf4j
@Getter
@Component
public class DefaultEdgeSynchronizationManager implements EdgeSynchronizationManager {

    private final ThreadLocal<EdgeId> edgeId = new ThreadLocal<>();

}
