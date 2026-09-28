// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventExportData extends EntityExportData<SchedulerEvent> {

    public static final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public JsonNode prepareConfiguration(JsonNode configuration, String type, Function<EntityId, EntityId> idMapper, UserId userId) {
        return switch (type) {
            case "updateFirmware", "updateSoftware" -> {
                ObjectNode msgBody = configuration.withObject("msgBody");
                String oldId = msgBody.path("id").asText(null);
                if (oldId != null) {
                    OtaPackageId otaPackageId = new OtaPackageId(UUID.fromString(oldId));
                    msgBody.put("id", idMapper.apply(otaPackageId).getId().toString());
                }
                yield configuration;
            }
            case "generateDashboardReport" -> {
                ObjectNode reportConfig = configuration.withObject("msgBody").withObject("reportConfig");
                reportConfig.put("userId", userId.getId().toString());
                String oldId = reportConfig.path("dashboardId").asText(null);
                if (oldId != null) {
                    DashboardId dashboardId = new DashboardId(UUID.fromString(oldId));
                    reportConfig.put("dashboardId", idMapper.apply(dashboardId).getId().toString());
                }
                yield configuration;
            }
            case "generateReport" -> {
                ReportConfig reportConfig = mapper.treeToValue(configuration, ReportConfig.class);
                reportConfig.setUserId(userId);
                reportConfig.setReportTemplateId((ReportTemplateId) idMapper.apply(reportConfig.getReportTemplateId()));
                reportConfig.setTargets(null);
                reportConfig.setNotificationTemplateId(null);
                yield mapper.valueToTree(reportConfig);
            }
            default -> configuration;
        };
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }
}
