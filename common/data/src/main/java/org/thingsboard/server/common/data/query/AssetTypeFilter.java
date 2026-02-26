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
package org.thingsboard.server.common.data.query;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode
@Setter
public class AssetTypeFilter implements EntityFilter {

    /**
     * Replaced by {@link AssetTypeFilter#getAssetTypes()} instead.
     */
    @Deprecated(since = "3.5", forRemoval = true)
    private String assetType;

    private List<String> assetTypes;

    public List<String> getAssetTypes() {
        return !CollectionUtils.isEmpty(assetTypes) ? assetTypes : Collections.singletonList(assetType);
    }

    @Getter
    private String assetNameFilter;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ASSET_TYPE;
    }

}
