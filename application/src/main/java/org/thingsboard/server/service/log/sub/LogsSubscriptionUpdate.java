// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.log.sub;

import lombok.Data;

import java.util.List;

@Data
public class LogsSubscriptionUpdate {

    private final long latestSeq;
    private final List<String> lines;
    private final int droppedLines;
    private final int evictedChunks;

}
