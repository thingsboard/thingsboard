// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

@Data
@AllArgsConstructor
public class DebugTbRateLimits {

    private TbRateLimits tbRateLimits;
    private boolean ruleChainEventSaved;

}
