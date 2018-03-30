package org.thingsboard.server.service.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ashvayka on 27.03.18.
 */
@Service
@Slf4j
public class DefaultTelemetrySubscriptionService implements TelemetrySubscriptionService {

    @Autowired
    private TelemetryWebSocketService wsService;


    private final Map<EntityId, Set<Subscription>> subscriptionsByEntityId = new HashMap<>();

    private final Map<String, Map<Integer, Subscription>> subscriptionsByWsSessionId = new HashMap<>();



    @Override
    public void onAttributesUpdateFromServer(EntityId entityId, String scope, List<AttributeKvEntry> attributes) {

    }

    @Override
    public void onTimeseriesUpdateFromServer(EntityId entityId, List<TsKvEntry> entries) {

    }
}
