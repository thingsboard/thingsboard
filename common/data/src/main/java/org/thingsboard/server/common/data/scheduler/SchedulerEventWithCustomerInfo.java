// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.scheduler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.SchedulerEventId;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SchedulerEventWithCustomerInfo extends SchedulerEventInfo {

    @Schema(description = "Title of the customer", example = "Company A")
    private String customerTitle;

    @Schema(description = "Parameter that specifies if customer is public", accessMode = Schema.AccessMode.READ_ONLY, type = "boolean")
    private boolean customerIsPublic;

    private List<Long> timestamps;

    public SchedulerEventWithCustomerInfo() {
        super();
    }

    public SchedulerEventWithCustomerInfo(SchedulerEventId schedulerEventId) {
        super(schedulerEventId);
    }

    public SchedulerEventWithCustomerInfo(SchedulerEventInfo schedulerEventInfo, String customerTitle, boolean customerIsPublic) {
        super(schedulerEventInfo);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }

}
