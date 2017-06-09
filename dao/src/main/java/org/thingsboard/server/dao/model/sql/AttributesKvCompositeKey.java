package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class AttributesKvCompositeKey implements Serializable {
    private String entityType;
    private UUID entityId;
    private String attributeType;
    private String attributeKey;
}
