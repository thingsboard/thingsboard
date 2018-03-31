package org.thingsboard.server.service.telemetry;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionState;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface TelemetrySubscriptionService {

    void onAttributesUpdateFromServer(EntityId entityId, String scope, List<AttributeKvEntry> attributes);

    void onTimeseriesUpdateFromServer(EntityId entityId, List<TsKvEntry> entries);

    void cleanupLocalWsSessionSubscriptions(TelemetryWebSocketSessionRef sessionRef, String sessionId);

    void removeSubscription(String sessionId, int cmdId);

    void addLocalWsSubscription(String sessionId, EntityId entityId, SubscriptionState sub);

    void onLocalTimeseriesUpdate(EntityId entityId, List<TsKvEntry> ts);

    void onLocalAttributesUpdate(EntityId entityId, String scope, Set<AttributeKvEntry> attributes);
}
