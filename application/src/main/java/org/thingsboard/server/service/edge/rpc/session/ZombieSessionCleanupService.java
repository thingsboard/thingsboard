// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.session;

import org.thingsboard.server.service.edge.rpc.session.manager.EdgeGrpcSessionManager;

public interface ZombieSessionCleanupService {

    void add(EdgeGrpcSessionManager session);
}
