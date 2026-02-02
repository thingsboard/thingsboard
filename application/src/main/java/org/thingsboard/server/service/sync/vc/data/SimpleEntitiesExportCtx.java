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
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;

public class SimpleEntitiesExportCtx extends EntitiesExportCtx<SingleEntityVersionCreateRequest> {

    @Getter
    private final EntityExportSettings settings;

    public SimpleEntitiesExportCtx(User user, CommitGitRequest commit, SingleEntityVersionCreateRequest request) {
        this(user, commit, request, request != null ? buildExportSettings(request.getConfig()) : null);
    }

    public SimpleEntitiesExportCtx(User user, CommitGitRequest commit, SingleEntityVersionCreateRequest request, EntityExportSettings settings) {
        super(user, commit, request);
        this.settings = settings;
    }
}
