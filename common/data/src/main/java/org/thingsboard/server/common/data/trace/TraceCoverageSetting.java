// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@Schema
public class TraceCoverageSetting implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_TRACES_PER_INTERVAL = 10;
    public static final int DEFAULT_TRACES_PER_PACK = 1;
    public static final int DEFAULT_INTERVAL_SECONDS = 1;
    public static final int DEFAULT_RELATED_TRACE_SAMPLE_INTERVAL_SECONDS = 1800;
    public static final MessagePayloadRecording DEFAULT_MESSAGE_PAYLOAD_RECORDING = MessagePayloadRecording.NONE;
    public static final int DEFAULT_MAX_TRACE_GROUPS_PER_DAY = 10000;


    public enum MessagePayloadRecording {
        NONE,
        FIRST_SPAN,
        ALL_SPANS
    }

    @Schema(description = "The Id of the Trace Coverage Setting, auto-generated, UUID", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID id;

    @Schema(description = "Id of the owning tenant", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID tenantId;

    @Schema(description = "Timestamp of the setting creation, in milliseconds", accessMode = Schema.AccessMode.READ_ONLY)
    private long createdTime;

    @Schema(description = "Whether trace coverage is active for this tenant", example = "false")
    private boolean enabled;

    @Schema(description = "How many traces to admit per interval (one message per admitted trace group). " +
            "The limit applies to each rule-engine instance separately", example = "10")
    private int tracesPerInterval = DEFAULT_TRACES_PER_INTERVAL;

    @Schema(description = "Max traced messages per pack per tenant; 0 means unlimited (no grouping by message type)", example = "1")
    private int tracesPerPack = DEFAULT_TRACES_PER_PACK;

    @Schema(description = "Length of the trace budget window, in seconds", example = "1")
    private int intervalSeconds = DEFAULT_INTERVAL_SECONDS;

    @Schema(description = "Minimum spacing (seconds) between related traces persisted per path, so drill-down trace coverage is spread over time", example = "1800")
    private int relatedTraceSampleIntervalSeconds = DEFAULT_RELATED_TRACE_SAMPLE_INTERVAL_SECONDS;

    @Schema(description = "Controls whether message data and metadata are recorded in trace spans", example = "NONE")
    private MessagePayloadRecording messagePayloadRecording = DEFAULT_MESSAGE_PAYLOAD_RECORDING;

    @Schema(description = "Max distinct trace groups (execution paths) tracked per tenant per day; once reached, traces on new paths " +
            "are not persisted while already-tracked paths keep aggregating. 0 means unlimited.", example = "10000")
    private int maxTraceGroupsPerDay = DEFAULT_MAX_TRACE_GROUPS_PER_DAY;

    public static TraceCoverageSetting defaultSetting(UUID tenantId) {
        TraceCoverageSetting setting = new TraceCoverageSetting();
        setting.setTenantId(tenantId);
        setting.setEnabled(false);
        setting.setTracesPerInterval(DEFAULT_TRACES_PER_INTERVAL);
        setting.setTracesPerPack(DEFAULT_TRACES_PER_PACK);
        setting.setIntervalSeconds(DEFAULT_INTERVAL_SECONDS);
        setting.setRelatedTraceSampleIntervalSeconds(DEFAULT_RELATED_TRACE_SAMPLE_INTERVAL_SECONDS);
        setting.setMessagePayloadRecording(DEFAULT_MESSAGE_PAYLOAD_RECORDING);
        setting.setMaxTraceGroupsPerDay(DEFAULT_MAX_TRACE_GROUPS_PER_DAY);
        return setting;
    }
}
