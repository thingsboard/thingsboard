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
package org.thingsboard.server.dao.exception;

import org.thingsboard.server.common.data.EntityType;

/**
 * Single source of truth for the "entity already exists" messages.
 * <p>Every message names the conflicting entity, so that the user can tell which one of the entities being saved
 * (or installed, or imported) caused the conflict. Keeping the text here also keeps the message identical no matter
 * whether the conflict was reported by an application layer validator or by the DB unique constraint that guards
 * the same field - both paths are hit for the same entity types, and a couple of find-or-create flows recognize the
 * conflict by comparing the message.
 */
public final class EntityConflictMessages {

    public static final String NAME = "name";
    public static final String TITLE = "title";

    private EntityConflictMessages() {
    }

    public static String alreadyExists(EntityType entityType, String field, Object value) {
        return entityType.getNormalName() + " with " + field + " " + quote(value) + " already exists!";
    }

    private static String quote(Object value) {
        return "\"" + value + "\"";
    }

}
