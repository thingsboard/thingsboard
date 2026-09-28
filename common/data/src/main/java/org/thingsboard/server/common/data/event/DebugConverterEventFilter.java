// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.event;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
public class DebugConverterEventFilter extends DebugEventFilter {

    @Override
    public EventType getEventType() {
        return EventType.DEBUG_CONVERTER;
    }

    private String type;
    private String in;
    private String out;
    private String metadata;

    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(type) || !StringUtils.isEmpty(in) || !StringUtils.isEmpty(out) || !StringUtils.isEmpty(metadata);
    }
}
