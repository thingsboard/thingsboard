// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.logexternal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String unitId;
    private List<String> lines;
    private int droppedLines;
    private long lastLineTs;
    /**
     * Buffer-assigned, unit-scoped sequence number. Set by {@code LogChunkBuffer.append} and restored when the chunk
     * is read back, so a replayed chunk can be stamped with its own seq rather than the tail watermark. 0 when the
     * chunk has not been through a buffer yet.
     */
    private long seq;

    public LogChunk(String unitId, List<String> lines, int droppedLines, long lastLineTs) {
        this(unitId, lines, droppedLines, lastLineTs, 0);
    }

}
