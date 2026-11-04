// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.downlink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.DownLinkMsg;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by ashvayka on 22.02.18.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DefaultDownlinkCacheService implements DownlinkCacheService {

    private final TbTransactionalCache<DownlinkCacheKey, DownLinkMsg> cache;

    @Override
    public DownLinkMsg get(IntegrationId integrationId, EntityId entityId) {
        return cache.getAndPutInTransaction(new DownlinkCacheKey(integrationId, entityId), () -> null, true);
    }

    @Override
    public DownLinkMsg put(IntegrationDownlinkMsg msg) {
        return getAndMerge(msg, DownLinkMsg::from, DownLinkMsg::merge);
    }

    @Override
    public void remove(IntegrationId integrationId, EntityId entityId) {
        cache.evict(new DownlinkCacheKey(integrationId, entityId));
    }

    private <T extends IntegrationDownlinkMsg> DownLinkMsg getAndMerge(T msg, Function<T, DownLinkMsg> from, BiFunction<DownLinkMsg, T, DownLinkMsg> merge) {
        var cacheKey = new DownlinkCacheKey(msg.getIntegrationId(), msg.getEntityId());
        var cacheValue = cache.get(cacheKey);

        DownLinkMsg result = cacheValue != null ? cacheValue.get() : null;

        if (result == null) {
            result = from.apply(msg);
        } else {
            result = merge.apply(result, msg);
        }

        cache.put(cacheKey, result);
        return result;
    }
}
