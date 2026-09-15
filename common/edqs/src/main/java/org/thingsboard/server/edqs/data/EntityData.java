// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.UUID;

public interface EntityData<T extends EntityFields> {

    UUID getId();

    EntityType getEntityType();

    UUID getCustomerId();

    void setCustomerId(UUID customerId);

    void setRepo(TenantRepo repo);

    T getFields();

    void setFields(T fields);

    DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType);

    boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value);

    boolean removeAttr(Integer keyId, AttributeScope scope);

    DataPoint getTs(Integer keyId);

    boolean putTs(Integer keyId, DataPoint value);

    boolean removeTs(Integer keyId);

    String getOwnerName();

    String getOwnerType();

    DataPoint getDataPoint(DataKey key, QueryContext queryContext);

    String getField(String name);

    boolean isEmpty();

}
