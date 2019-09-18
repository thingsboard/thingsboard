/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.model.sqlts.util;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.sessions.Session;

import java.util.UUID;

public class UUIDConverter implements Converter {

    private static final String TYPE_NAME = "java.util.UUID";
    private static final String COL_DEFINITION = "UUID";

    @Override
    public Object convertObjectValueToDataValue(Object objectValue, Session session) {
        return objectValue;
    }

    @Override
    public UUID convertDataValueToObjectValue(Object dataValue, Session session) {
        return (UUID) dataValue;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public void initialize(DatabaseMapping databaseMapping, Session session) {
        final DatabaseField field;
        if (databaseMapping instanceof DirectCollectionMapping) {
            // handle @ElementCollection...
            field = ((DirectCollectionMapping) databaseMapping).getDirectField();
        } else {
            field = databaseMapping.getField();
        }
        field.setSqlType(java.sql.Types.OTHER);
        field.setTypeName(TYPE_NAME);
        field.setColumnDefinition(COL_DEFINITION);
    }
}