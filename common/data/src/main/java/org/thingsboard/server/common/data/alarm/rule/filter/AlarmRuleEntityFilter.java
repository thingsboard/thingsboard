/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data.alarm.rule.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AlarmRuleSingleEntityFilter.class, name = "SINGLE_ENTITY"),
        @JsonSubTypes.Type(value = AlarmRuleDeviceTypeEntityFilter.class, name = "DEVICE_TYPE"),
        @JsonSubTypes.Type(value = AlarmRuleAssetTypeEntityFilter.class, name = "ASSET_TYPE"),
        @JsonSubTypes.Type(value = AlarmRuleEntityListEntityFilter.class, name = "ENTITY_LIST")})
public interface AlarmRuleEntityFilter extends Serializable {

    @JsonIgnore
    AlarmRuleEntityFilterType getType();

    boolean isEntityMatches(EntityId entityId);

}
