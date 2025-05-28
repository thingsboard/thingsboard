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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdqsState {

    private Boolean edqsReady;
    @Setter
    private EdqsSyncStatus syncStatus;
    @Setter
    private EdqsApiMode apiMode;

    public boolean updateEdqsReady(boolean ready) {
        boolean changed = BooleanUtils.toBooleanDefaultIfNull(this.edqsReady, false) != ready;
        this.edqsReady = ready;
        return changed;
    }

    @JsonIgnore
    public boolean isApiReady() {
        return edqsReady && syncStatus == EdqsSyncStatus.FINISHED;
    }

    @JsonIgnore
    public boolean isApiEnabled() {
        return apiMode != null && (apiMode == EdqsApiMode.ENABLED || apiMode == EdqsApiMode.AUTO_ENABLED);
    }

    @Override
    public String toString() {
        return '[' +
               "EDQS ready: " + edqsReady +
               ", sync status: " + syncStatus +
               ", API mode: " + apiMode +
               ']';
    }

    public enum EdqsSyncStatus {
        REQUESTED,
        STARTED,
        FINISHED,
        FAILED
    }

    public enum EdqsApiMode {
        ENABLED,
        AUTO_ENABLED,
        DISABLED,
        AUTO_DISABLED
    }

}
