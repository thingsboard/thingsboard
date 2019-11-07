package org.thingsboard.server.common.data.edge;

import lombok.Data;
import org.thingsboard.server.common.data.EntityType;

@Data
public class EdgeQueueEntry {
    private String type;
    private EntityType entityType;
    private String data;
}
