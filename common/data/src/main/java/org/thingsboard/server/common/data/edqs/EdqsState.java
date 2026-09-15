// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

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
        boolean changed = toBooleanDefaultIfNull(this.edqsReady, false) != ready;
        this.edqsReady = ready;
        return changed;
    }

    @JsonIgnore
    public boolean isApiReady() {
        return toBooleanDefaultIfNull(edqsReady, false) && syncStatus == EdqsSyncStatus.FINISHED;
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
