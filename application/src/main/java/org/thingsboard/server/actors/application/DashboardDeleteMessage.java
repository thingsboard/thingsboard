package org.thingsboard.server.actors.application;

import lombok.Getter;
import org.thingsboard.server.common.data.id.DashboardId;

public class DashboardDeleteMessage {
    @Getter
    private final DashboardId dashboardId;

    public DashboardDeleteMessage(DashboardId dashboardId) {
        this.dashboardId = dashboardId;
    }
}
