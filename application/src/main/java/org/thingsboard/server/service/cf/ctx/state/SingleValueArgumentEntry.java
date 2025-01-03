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
package org.thingsboard.server.service.cf.ctx.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleValueArgumentEntry implements ArgumentEntry {

    public static final ArgumentEntry EMPTY = new SingleValueArgumentEntry(0);

    private long ts;
    private Object value;

    private Long version;

    public SingleValueArgumentEntry(KvEntry entry) {
        if (entry instanceof TsKvEntry tsKvEntry) {
            this.ts = tsKvEntry.getTs();
            this.version = tsKvEntry.getVersion();
        } else if (entry instanceof AttributeKvEntry attributeKvEntry) {
            this.ts = attributeKvEntry.getLastUpdateTs();
            this.version = attributeKvEntry.getVersion();
        }
        this.value = entry.getValue();
    }

    /**
     * Internal constructor to create immutable SingleValueArgumentEntry.EMPTY
     * */
    private SingleValueArgumentEntry(int ignored) {
        this.ts = System.currentTimeMillis();
        this.value = null;
    }

    @Override
    public ArgumentEntryType getType() {
        return ArgumentEntryType.SINGLE_VALUE;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean hasUpdatedValue(ArgumentEntry entry) {
        return this.ts != ((SingleValueArgumentEntry) entry).getTs();
    }

    @Override
    public ArgumentEntry copy() {
        return new SingleValueArgumentEntry(this.ts, this.value, this.version);
    }

}
