// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws.telemetry.cmd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUnsubscribeCmd;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link WsCommandsWrapper}. This class is left for backward compatibility
 * */
@Data
@Deprecated
public class TelemetryCmdsWrapper {

    private List<AttributesSubscriptionCmd> attrSubCmds;

    private List<TimeseriesSubscriptionCmd> tsSubCmds;

    private List<GetHistoryCmd> historyCmds;

    private List<EntityDataCmd> entityDataCmds;

    private List<EntityDataUnsubscribeCmd> entityDataUnsubscribeCmds;

    private List<AlarmDataCmd> alarmDataCmds;

    private List<AlarmDataUnsubscribeCmd> alarmDataUnsubscribeCmds;

    private List<EntityCountCmd> entityCountCmds;

    private List<EntityCountUnsubscribeCmd> entityCountUnsubscribeCmds;

    private List<AlarmCountCmd> alarmCountCmds;

    private List<AlarmCountUnsubscribeCmd> alarmCountUnsubscribeCmds;

    @JsonIgnore
    public WsCommandsWrapper toCommonCmdsWrapper() {
        return new WsCommandsWrapper(null, Stream.of(
                        attrSubCmds, tsSubCmds, historyCmds, entityDataCmds,
                        entityDataUnsubscribeCmds, alarmDataCmds, alarmDataUnsubscribeCmds,
                        entityCountCmds, entityCountUnsubscribeCmds,
                        alarmCountCmds, alarmCountUnsubscribeCmds
                )
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
    }

}
