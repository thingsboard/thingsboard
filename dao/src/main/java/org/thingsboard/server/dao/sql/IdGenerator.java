/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.thingsboard.server.dao.model.BaseEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.EnumSet;

@Slf4j
public class IdGenerator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        if (owner instanceof BaseEntity<?> entity && entity.getUuid() != null) {
            return entity.getUuid();
        }
        return Uuids.timeBased();
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @IdGeneratorType(IdGenerator.class)
    public @interface GeneratedId {
    }

}
