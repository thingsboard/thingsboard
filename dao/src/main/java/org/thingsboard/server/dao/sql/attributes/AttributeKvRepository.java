package org.thingsboard.server.dao.sql.attributes;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.model.sql.AttributesKvCompositeKey;

@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true")
public interface AttributeKvRepository extends CrudRepository<AttributeKvEntity, AttributesKvCompositeKey> {
}
