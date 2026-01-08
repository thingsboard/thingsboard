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
