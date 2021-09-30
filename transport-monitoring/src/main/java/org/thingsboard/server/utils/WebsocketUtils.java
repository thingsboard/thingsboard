package org.thingsboard.server.utils;

import org.thingsboard.server.TransportsMonitoringScheduler;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.telemetry.cmd.LatestValueCmd;
import org.thingsboard.server.common.data.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.common.data.telemetry.wrapper.TelemetryPluginCmdsWrapper;

import java.util.Collections;
import java.util.UUID;

public class WebsocketUtils {
    public static TelemetryPluginCmdsWrapper getTelemetryCmdsWrapper(UUID uuid) {
        DeviceId deviceId = new DeviceId(uuid);
        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(deviceId);
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, TransportsMonitoringScheduler.PAYLOAD_KEY_STR)));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        TelemetryPluginCmdsWrapper wrapper = new TelemetryPluginCmdsWrapper();
        wrapper.setEntityDataCmds(Collections.singletonList(cmd));
        return wrapper;
    }
}
