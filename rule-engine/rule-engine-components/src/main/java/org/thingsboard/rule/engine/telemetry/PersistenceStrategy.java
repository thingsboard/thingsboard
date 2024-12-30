/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SaveEveryMessagePersistenceStrategy.class, name = "SAVE_EVERY_MESSAGE"),
        @JsonSubTypes.Type(value = SaveFirstInIntervalPersistenceStrategy.class, name = "SAVE_FIRST_IN_INTERVAL"),
        @JsonSubTypes.Type(value = DoNotSavePersistenceStrategy.class, name = "DO_NOT_SAVE")
})
public sealed interface PersistenceStrategy permits DoNotSavePersistenceStrategy, SaveEveryMessagePersistenceStrategy, SaveFirstInIntervalPersistenceStrategy {

    // TODO: maybe this should accept generic key?
    boolean shouldPersist(UUID originatorUuid, TsKvEntry timeseriesEntry);

}
