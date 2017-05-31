package org.thingsboard.server.dao.sql.attributes;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.attributes.AttributesDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true")
public class JpaAttributesDao implements AttributesDao {

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(EntityId entityId, String attributeType, String attributeKey) {
        return null;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(EntityId entityId, String attributeType, Collection<String> attributeKey) {
        return null;
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(EntityId entityId, String attributeType) {
        return null;
    }

    @Override
    public ResultSetFuture save(EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        return null;
    }

    @Override
    public ListenableFuture<List<ResultSet>> removeAll(EntityId entityId, String scope, List<String> keys) {
        return null;
    }
}
