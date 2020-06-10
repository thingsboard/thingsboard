package org.thingsboard.server.service.telemetry.cmd.v2;

import lombok.Data;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityData;

import java.util.List;

@Data
public class EntityDataUpdate {

    private final int cmdId;
    private final PageData<EntityData> data;
    private final List<EntityData> update;
    private int errorCode;
    private String errorMsg;
}
