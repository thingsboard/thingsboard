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

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
public interface AttributesService {

    AttributeKvEntry find(EntityId entityId, String scope, String attributeKey);

    List<AttributeKvEntry> findAll(EntityId entityId, String scope);

    ListenableFuture<List<ResultSet>> save(EntityId entityId, String scope, List<AttributeKvEntry> attributes);

    void removeAll(EntityId entityId, String scope, List<String> attributeKeys);
}
