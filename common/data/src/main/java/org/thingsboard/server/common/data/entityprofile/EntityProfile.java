/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.entityprofile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = {"profile", "profileBytes", "additionalInfo", "additionalInfoBytes"})
public class EntityProfile extends BaseData<EntityProfileId> implements HasName, HasTenantId {
    private EntityProfileId id;
    private String name;
    private TenantId tenantId;
    private EntityType entityType;
    private transient JsonNode profile;
    @JsonIgnore
    private byte[] profileBytes;
    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    public static class EntityProfileBuilder {
        public EntityProfileBuilder profile(JsonNode profile) {
            SearchTextBasedWithAdditionalInfo.setJson(profile, json -> this.profile = json, bytes -> this.profileBytes = bytes);
            return this;
        }

        public EntityProfileBuilder additionalInfo(JsonNode addInfo) {
            SearchTextBasedWithAdditionalInfo.setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
            return this;
        }
    }

    public JsonNode getProfile() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> profile, () -> profileBytes);
    }

    public JsonNode getAdditionalInfo() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }
}
