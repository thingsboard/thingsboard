// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
