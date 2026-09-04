// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema
public class RuleChainDebugEventFilter extends DebugEventFilter {

    @Schema(description = "String value representing the message")
    protected String message;

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_RULE_CHAIN;
    }

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(message);
    }
}
