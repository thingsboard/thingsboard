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
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ATTRIBUTE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class AttributeKvCompositeKey implements Serializable {
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;
    @Column(name = ATTRIBUTE_TYPE_COLUMN)
    private int attributeType;
    @Column(name = ATTRIBUTE_KEY_COLUMN)
    private int attributeKey;
}
