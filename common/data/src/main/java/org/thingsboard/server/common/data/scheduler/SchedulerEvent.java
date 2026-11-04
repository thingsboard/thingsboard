// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.SchedulerEventId;

import java.io.Serial;

import static org.thingsboard.server.common.data.DataConstants.GENERATE_DASHBOARD_REPORT;
import static org.thingsboard.server.common.data.DataConstants.GENERATE_REPORT;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SchedulerEvent extends SchedulerEventInfo {

    @Serial
    private static final long serialVersionUID = 2807343050519549363L;

    @Schema(description = "a JSON value with scheduler event configuration", implementation = com.fasterxml.jackson.databind.JsonNode.class)
    private transient JsonNode configuration;
    @JsonIgnore
    private byte[] configurationBytes;

    public SchedulerEvent() {
        super();
    }

    public SchedulerEvent(SchedulerEventId id) {
        super(id);
    }

    public SchedulerEvent(SchedulerEvent schedulerEvent) {
        super(schedulerEvent);
        this.setConfiguration(schedulerEvent.getConfiguration().deepCopy());
    }

    public SchedulerEvent(SchedulerEventInfo schedulerEventInfo, JsonNode configuration) {
        super(schedulerEventInfo);
        this.setConfiguration(configuration.deepCopy());
    }

    public JsonNode getConfiguration() {
        return BaseDataWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

    @JsonIgnore
    public boolean isReportProducing() {
        return isReportProducing(getType(), getConfiguration());
    }

    /**
     * Whether an event described by this type and configuration generates reports once persisted.
     * Every write path that persists such an event must answer this the same way.
     * <p>
     * The raw type as well as the resolved message type, because the two report types are dispatched
     * differently: {@code GENERATE_REPORT} is submitted as a report job on the event type alone, so a
     * {@code msgType} in the configuration does not stop it generating reports and consulting only the
     * resolved value would wave it through, while {@code GENERATE_DASHBOARD_REPORT} is reached through the
     * resolved value. Answering on the union is the conservative reading: an event that produces a report by
     * either route counts as producing one.
     */
    public static boolean isReportProducing(String type, JsonNode configuration) {
        return isReportMsgType(type) || isReportMsgType(resolveMsgType(type, configuration));
    }

    private static boolean isReportMsgType(String msgType) {
        return GENERATE_REPORT.equals(msgType) || GENERATE_DASHBOARD_REPORT.equals(msgType);
    }

    /**
     * The message type the rule-engine branch of the scheduler dispatches on: the configuration's
     * {@code msgType} when present, the event type otherwise. The report-job branch is chosen by the event
     * type alone, so what this answers has no bearing on it.
     */
    public static String resolveMsgType(String type, JsonNode configuration) {
        if (configuration != null && configuration.hasNonNull("msgType")) {
            return configuration.get("msgType").asText();
        }
        return type;
    }

}
