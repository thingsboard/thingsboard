/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.attributes;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
@Service
public class BaseAttributesService implements AttributesService {

    @Autowired
    private AttributesDao attributesDao;

    @Override
    public AttributeKvEntry find(EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);
        return attributesDao.find(entityId, scope, attributeKey);
    }

    @Override
    public List<AttributeKvEntry> findAll(EntityId entityId, String scope) {
        validate(entityId, scope);
        return attributesDao.findAll(entityId, scope);
    }

    @Override
    public ListenableFuture<List<ResultSet>> save(EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        attributes.forEach(attribute -> validate(attribute));
        List<ResultSetFuture> futures = Lists.newArrayListWithExpectedSize(attributes.size());
        for(AttributeKvEntry attribute : attributes) {
            futures.add(attributesDao.save(entityId, scope, attribute));
        }
        return Futures.allAsList(futures);
    }

    @Override
    public void removeAll(EntityId entityId, String scope, List<String> keys) {
        validate(entityId, scope);
        attributesDao.removeAll(entityId, scope, keys);
    }

    private static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), "Incorrect id " + id);
        Validator.validateString(scope, "Incorrect scope " + scope);
    }

    private static void validate(AttributeKvEntry kvEntry) {
        if (kvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        } else if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }

}
