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
package org.thingsboard.server.common.data.cf.configuration.aggregation;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;
import java.util.function.Predicate;

@Data
@Builder
public class CfAggTrigger {

    private List<EntityId> entityProfiles;
    private List<ReferencedEntityKey> inputs;

    public boolean matches(EntityId profileId, Predicate<CfAggTrigger> cfAggTrigger) {
        if (matchesProfile(profileId)) {
            return cfAggTrigger.test(this);
        }
        return false;
    }

    public boolean matchesProfile(EntityId profileId) {
        return entityProfiles.isEmpty() || entityProfiles.contains(profileId);
    }

    public boolean matchesTimeSeries(List<TsKvEntry> telemetry) {
        if (telemetry == null || telemetry.isEmpty()) {
            return false;
        }
        for (TsKvEntry tsKvEntry : telemetry) {
            ReferencedEntityKey latestKey = new ReferencedEntityKey(tsKvEntry.getKey(), ArgumentType.TS_LATEST, null);
            if (inputs.contains(latestKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesAttributes(List<AttributeKvEntry> attributes, AttributeScope scope) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }
        for (AttributeKvEntry attributeKvEntry : attributes) {
            ReferencedEntityKey latestKey = new ReferencedEntityKey(attributeKvEntry.getKey(), ArgumentType.ATTRIBUTE, scope);
            if (inputs.contains(latestKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesTimeSeriesKeys(List<String> telemetry) {
        if (telemetry == null || telemetry.isEmpty()) {
            return false;
        }

        for (String key : telemetry) {
            ReferencedEntityKey latestKey = new ReferencedEntityKey(key, ArgumentType.TS_LATEST, null);
            if (inputs.contains(latestKey)) {
                return true;
            }
        }

        return false;
    }

    public boolean matchesAttributesKeys(List<String> attributes, AttributeScope scope) {
        if (attributes == null || attributes.isEmpty()) {
            return false;
        }

        for (String key : attributes) {
            ReferencedEntityKey latestKey = new ReferencedEntityKey(key, ArgumentType.ATTRIBUTE, scope);
            if (inputs.contains(latestKey)) {
                return true;
            }
        }

        return false;
    }

}
