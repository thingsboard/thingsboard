// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.List;

@ToString
@Getter
public class LogsUpdate extends CmdUpdate {

    private final long latestSeq;
    private final List<String> lines;
    private final int droppedLines;
    private final int evictedChunks;

    public LogsUpdate(int cmdId, long latestSeq, List<String> lines, int droppedLines, int evictedChunks) {
        super(cmdId, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.latestSeq = latestSeq;
        this.lines = lines;
        this.droppedLines = droppedLines;
        this.evictedChunks = evictedChunks;
    }

    @Builder
    public LogsUpdate(@JsonProperty("cmdId") int cmdId,
                      @JsonProperty("latestSeq") long latestSeq,
                      @JsonProperty("lines") List<String> lines,
                      @JsonProperty("droppedLines") int droppedLines,
                      @JsonProperty("evictedChunks") int evictedChunks,
                      @JsonProperty("errorCode") int errorCode,
                      @JsonProperty("errorMsg") String errorMsg) {
        super(cmdId, errorCode, errorMsg);
        this.latestSeq = latestSeq;
        this.lines = lines != null ? lines : List.of();
        this.droppedLines = droppedLines;
        this.evictedChunks = evictedChunks;
    }

    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.LOGS;
    }

}
