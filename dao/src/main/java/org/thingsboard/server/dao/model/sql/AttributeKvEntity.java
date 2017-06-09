package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = "attributes_kv")
public class AttributeKvEntity implements ToData<AttributeKvEntry> {

    @Id
    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    @Id
    @Column(name = ENTITY_ID_COLUMN)
    private UUID entityId;

    @Id
    @Column(name = ATTRIBUTE_TYPE_COLUMN)
    private String attributeType;

    @Id
    @Column(name = ATTRIBUTE_KEY_COLUMN)
    private String attributeKey;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    private String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    private Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    @Column(name = LAST_UPDATE_TS_COLUMN)
    private Long lastUpdateTs;

    @Override
    public AttributeKvEntry toData() {
        // TODO - add implementation
        return null;
    }
}
