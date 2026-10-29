// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import static org.thingsboard.server.common.data.DataConstants.GENERATE_REPORT;

@Slf4j
public abstract class BaseSchedulerEventProcessor extends BaseEdgeProcessor {

    @Autowired
    private DataValidator<SchedulerEvent> schedulerEventValidator;

    protected Boolean saveOrUpdateSchedulerEvent(TenantId tenantId, SchedulerEventId schedulerEventId, SchedulerEventUpdateMsg schedulerEventUpdateMsg) {
        boolean created = false;
        try {
            SchedulerEvent schedulerEvent = JacksonUtil.fromString(schedulerEventUpdateMsg.getEntity(), SchedulerEvent.class, true);
            if (schedulerEvent == null) {
                throw new RuntimeException("[{" + tenantId + "}] schedulerEventUpdateMsg {" + schedulerEventUpdateMsg + "} cannot be converted to scheduler event");
            }
            checkEntityTenant(tenantId, schedulerEventId);
            schedulerEvent.setTenantId(tenantId);
            checkReportConfig(tenantId, schedulerEvent);
            SchedulerEvent existingSchedulerEvent = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, schedulerEventId);
            if (existingSchedulerEvent == null) {
                created = true;
                schedulerEvent.setId(null);
            }
            schedulerEventValidator.validate(schedulerEvent, SchedulerEventInfo::getTenantId);
            if (created) {
                updateEnabledBasedOnCreationRules(schedulerEvent);
                schedulerEvent.setId(schedulerEventId);
            } else {
                schedulerEvent.setEnabled(existingSchedulerEvent.isEnabled());
            }
            edgeCtx.getSchedulerEventService().saveSchedulerEvent(schedulerEvent, false);

        } catch (Exception e) {
            log.error("[{}] Failed to process scheduler event update msg [{}]", tenantId, schedulerEventUpdateMsg, e);
            throw e;
        }
        return created;
    }

    private void checkReportConfig(TenantId tenantId, SchedulerEvent schedulerEvent) {
        if (GENERATE_REPORT.equals(schedulerEvent.getType())) {
            ReportConfig reportConfig = JacksonUtil.treeToValue(schedulerEvent.getConfiguration(), ReportConfig.class);
            if (reportConfig == null || reportConfig.getReportTemplateId() == null || !isEntityExists(tenantId, reportConfig.getReportTemplateId())
                    || reportConfig.getUserId() == null || !isEntityExists(tenantId, reportConfig.getUserId())) {
                throw new DataValidationException("Scheduler event is referencing to non-existent report template or user!");
            }
        }
    }

    private void updateEnabledBasedOnCreationRules(SchedulerEvent newSchedulerEvent) {
        boolean isEnabled = newSchedulerEvent.isEnabled() && isEnabledDuringCreation();
        newSchedulerEvent.setEnabled(isEnabled);
    }

    protected abstract boolean isEnabledDuringCreation();

    protected abstract void setCustomerId(TenantId tenantId, CustomerId customerId, SchedulerEvent schedulerEvent);
}
