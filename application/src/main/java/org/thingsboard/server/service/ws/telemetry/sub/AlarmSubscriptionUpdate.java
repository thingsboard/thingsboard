/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.ws.telemetry.sub;

import lombok.Getter;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

public class AlarmSubscriptionUpdate {

    @Getter
    private int subscriptionId;
    @Getter
    private int errorCode;
    @Getter
    private String errorMsg;
    @Getter
    private AlarmInfo alarm;
    @Getter
    private boolean alarmDeleted;

    public AlarmSubscriptionUpdate(int subscriptionId, AlarmInfo alarm) {
        this(subscriptionId, alarm, false);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, AlarmInfo alarm, boolean alarmDeleted) {
        super();
        this.subscriptionId = subscriptionId;
        this.alarm = alarm;
        this.alarmDeleted = alarmDeleted;
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode) {
        this(subscriptionId, errorCode, null);
    }

    public AlarmSubscriptionUpdate(int subscriptionId, SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.subscriptionId = subscriptionId;
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }

    @Override
    public String toString() {
        return "AlarmUpdate [subscriptionId=" + subscriptionId + ", errorCode=" + errorCode + ", errorMsg=" + errorMsg +
                ", alarm=" + alarm + "]";
    }
}