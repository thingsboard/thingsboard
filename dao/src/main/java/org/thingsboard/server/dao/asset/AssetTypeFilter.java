// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.asset;

import jakarta.annotation.Nullable;
import lombok.Data;

import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
public class AssetTypeFilter {
    @Nullable
    private String relationType;
    @Nullable
    private List<String> assetTypes;
}
