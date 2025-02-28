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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleValueArgumentEntry.class, name = "SINGLE_VALUE"),
        @JsonSubTypes.Type(value = TsRollingArgumentEntry.class, name = "TS_ROLLING")
})
public interface ArgumentEntry {

    @JsonIgnore
    ArgumentEntryType getType();

    Object getValue();

    boolean updateEntry(ArgumentEntry entry);

    boolean isEmpty();

    TbelCfArg toTbelCfArg();

    boolean isForceResetPrevious();

    void setForceResetPrevious(boolean forceResetPrevious);

    static ArgumentEntry createSingleValueArgument(KvEntry kvEntry) {
        return new SingleValueArgumentEntry(kvEntry);
    }

    static ArgumentEntry createTsRollingArgument(List<TsKvEntry> kvEntries, int limit, long timeWindow) {
        return new TsRollingArgumentEntry(kvEntries, limit, timeWindow);
    }

}
