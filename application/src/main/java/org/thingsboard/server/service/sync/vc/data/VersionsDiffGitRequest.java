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
package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.EntityVersionsDiff;

import java.util.List;

@Getter
public class VersionsDiffGitRequest extends PendingGitRequest<List<EntityVersionsDiff>> {

    private final String path;
    private final String versionId1;
    private final String versionId2;

    public VersionsDiffGitRequest(TenantId tenantId, String path, String versionId1, String versionId2) {
        super(tenantId);
        this.path = path;
        this.versionId1 = versionId1;
        this.versionId2 = versionId2;
    }

}
