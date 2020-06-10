package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

@Data
public class EntityData {

    private final EntityId entityId;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;
    private final Map<String, TsValue[]> timeseries;

}
