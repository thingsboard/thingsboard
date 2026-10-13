// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.IntegrationId;

@RequiredArgsConstructor
@Service
public class DefaultRemoteIntegrationSessionService implements RemoteIntegrationSessionService {

    private final TbTransactionalCache<IntegrationId, IntegrationSession> cache;

    @Override
    public IntegrationSession findIntegrationSession(IntegrationId integrationId) {
        return cache.getAndPutInTransaction(integrationId, () -> null, true);
    }

    @Override
    public IntegrationSession putIntegrationSession(IntegrationId integrationId, IntegrationSession session) {
        cache.put(integrationId, session);
        return session;
    }

    @Override
    public void removeIntegrationSession(IntegrationId integrationId) {
        cache.evict(integrationId);
    }
}
