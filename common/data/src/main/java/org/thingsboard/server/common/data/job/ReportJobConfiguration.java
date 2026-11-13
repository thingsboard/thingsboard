// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.rule.RuleNode;

import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(callSuper = true)
public class ReportJobConfiguration extends JobConfiguration {

    private ReportTemplateId reportTemplateId;
    private UserId userId;
    private String timezone;
    private boolean makePublic;

    private List<UUID> targets;
    private NotificationTemplateId notificationTemplateId;
    private List<NotificationRequest> notificationRequests;

    private EntityId originator;

    private RuleNode ruleNode;
    private String outputTbMsgProto;
    private String queueName;

    private EntityInfo schedulerEventInfo;

    @Override
    public JobType getType() {
        return JobType.REPORT;
    }

}
