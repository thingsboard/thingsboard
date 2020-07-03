package org.thingsboard.server.dao.alarm;

import lombok.Data;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@Data
public class AlarmOperationResult {
    private final Alarm alarm;
    private final boolean successful;
    private final List<EntityId> propagatedEntitiesList;
}
