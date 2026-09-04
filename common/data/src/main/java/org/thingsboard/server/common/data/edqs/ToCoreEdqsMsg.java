// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.edqs.EdqsState.EdqsSyncStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ToCoreEdqsMsg {

    private EdqsSyncRequest syncRequest;
    private Boolean apiEnabled;

    private EdqsSyncStatus syncStatus;
    private Boolean edqsReady;

}
