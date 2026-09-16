// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class VersionControlTaskCacheEntry implements Serializable {

    private static final long serialVersionUID = -7875992200801588119L;

    private VersionCreationResult exportResult;
    private VersionLoadResult importResult;

    public static VersionControlTaskCacheEntry newForExport(VersionCreationResult result) {
        return new VersionControlTaskCacheEntry(result, null);
    }

    public static VersionControlTaskCacheEntry newForImport(VersionLoadResult result) {
        return new VersionControlTaskCacheEntry(null, result);
    }


}
