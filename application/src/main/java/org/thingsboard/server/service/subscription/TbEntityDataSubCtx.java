package org.thingsboard.server.service.subscription;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.service.telemetry.TelemetryWebSocketSessionRef;
import org.thingsboard.server.service.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.TimeSeriesCmd;

@Data
public class TbEntityDataSubCtx {

    private final TelemetryWebSocketSessionRef sessionRef;
    private final int cmdId;
    private EntityDataQuery query;
    private LatestValueCmd latestCmd;
    private TimeSeriesCmd tsCmd;
    private PageData<EntityData> data;
    private boolean initialDataSent;

    public TbEntityDataSubCtx(TelemetryWebSocketSessionRef sessionRef, int cmdId) {
        this.sessionRef = sessionRef;
        this.cmdId = cmdId;
    }

    public String getSessionId() {
        return sessionRef.getSessionId();
    }

    public TenantId getTenantId() {
        return sessionRef.getSecurityCtx().getTenantId();
    }

    public CustomerId getCustomerId() {
        return sessionRef.getSecurityCtx().getCustomerId();
    }


    public void setData(PageData<EntityData> data) {
        this.data = data;
    }

}
