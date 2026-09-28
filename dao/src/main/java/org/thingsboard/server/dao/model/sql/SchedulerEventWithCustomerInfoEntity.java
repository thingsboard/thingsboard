// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class SchedulerEventWithCustomerInfoEntity extends AbstractSchedulerEventInfoEntity<SchedulerEventWithCustomerInfo> {

    public static final Map<String,String> schedulerEventWithCustomerInfoColumnMap = new HashMap<>();
    static {
        schedulerEventWithCustomerInfoColumnMap.put("customerTitle", "c.title");
    }

    private String customerTitle;
    private boolean customerIsPublic;

    public SchedulerEventWithCustomerInfoEntity() {
        super();
    }

    public SchedulerEventWithCustomerInfoEntity(SchedulerEventInfoEntity schedulerEventInfoEntity,
                                                String customerTitle,
                                                Object customerAdditionalInfo) {
        super(schedulerEventInfoEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
    }

    @Override
    public SchedulerEventWithCustomerInfo toData() {
        return new SchedulerEventWithCustomerInfo(super.toSchedulerEventInfo(), customerTitle, customerIsPublic);
    }
}
