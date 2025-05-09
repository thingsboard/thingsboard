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
package org.thingsboard.server.common.data.edqs;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;

@Getter
@NoArgsConstructor
public class EdqsState {

    private Boolean edqsReady;
    private EdqsSyncStatus syncStatus;

    private Boolean apiEnabled; // null until auto-enabled or set manually

    public boolean setEdqsReady(boolean ready) {
        boolean changed = BooleanUtils.toBooleanDefaultIfNull(this.edqsReady, false) != ready;
        this.edqsReady = ready;
        return changed;
    }

    public void setSyncStatus(EdqsSyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public boolean setApiEnabled(boolean apiEnabled) {
        boolean changed = BooleanUtils.toBooleanDefaultIfNull(this.apiEnabled, false) != apiEnabled;
        this.apiEnabled = apiEnabled;
        return changed;
    }

    public boolean isApiReady() {
        return edqsReady && syncStatus == EdqsSyncStatus.FINISHED;
    }

    public boolean isApiEnabled() {
        return apiEnabled != null && apiEnabled;
    }

    @Override
    public String toString() {
        return '[' +
               "EDQS ready: " + edqsReady +
               ", sync status: " + syncStatus +
               ", API enabled: " + apiEnabled +
               ']';
    }

    public enum EdqsSyncStatus {
        REQUESTED,
        STARTED,
        FINISHED,
        FAILED
    }

}
