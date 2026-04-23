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
package org.thingsboard.server.common.data.rule.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * One element of the {@code tb_acl} metadata JSON array. Describes which
 * {@code Operation} names (see the permission module) the calling user is allowed
 * to perform on the referenced entity. An empty {@link #allowed} list means the
 * caller has no role-level access to the entity's resource (or the entity's type
 * does not map to any known permission Resource).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityAclEntry {

    private EntityType entityType;
    private UUID entityId;
    private List<String> allowed;

}
