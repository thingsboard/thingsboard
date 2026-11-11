// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class ScheduledReportQuery {

    private PageLink pageLink;
    private UUID reportTemplateId;
    private UUID userId;
    private boolean includeCustomers;

}
