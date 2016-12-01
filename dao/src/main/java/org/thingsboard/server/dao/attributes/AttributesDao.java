/**
 * Copyright © 2016 The Thingsboard Authors
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

import com.datastax.driver.core.ResultSetFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
public interface AttributesDao {

    AttributeKvEntry find(EntityId entityId, String attributeType, String attributeKey);

    List<AttributeKvEntry> findAll(EntityId entityId, String attributeType);

    ResultSetFuture save(EntityId entityId, String attributeType, AttributeKvEntry attribute);

    void removeAll(EntityId entityId, String scope, List<String> keys);
}
