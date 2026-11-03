/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.attributes;

import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.EntityType;

/**
 * Entity type specific strategy of the attributes synchronization to edge.
 * Implementations decide whether a particular attributes update of the supported entity type must be
 * propagated to related edges and how to propagate it. See AttributesEdgeSyncService for the general rules
 * applied to all attribute updates before the strategy is consulted.
 */
public interface AttributesEdgeSyncStrategy {

    EntityType getEntityType();

    /**
     * Lightweight check executed synchronously on the attributes save path.
     * Implementations must not perform any blocking calls here (no DB or distributed cache lookups) -
     * in-memory checks only. Entity lookups must be deferred to the async part of the sync.
     */
    boolean isEdgeSyncRequired(AttributesSaveRequest request);

    /**
     * Lightweight check executed synchronously on the attributes delete path.
     * Implementations must not perform any blocking calls here (no DB or distributed cache lookups) -
     * in-memory checks only. Entity lookups must be deferred to the async part of the sync.
     */
    boolean isEdgeSyncRequired(AttributesDeleteRequest request);

    void onAttributesUpdate(AttributesSaveRequest request);

    void onAttributesDelete(AttributesDeleteRequest request);

}
