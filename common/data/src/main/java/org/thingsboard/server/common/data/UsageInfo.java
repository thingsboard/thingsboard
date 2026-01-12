/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.Data;

@Data
public class UsageInfo {

    private long devices;
    private long maxDevices;
    private long assets;
    private long maxAssets;
    private long customers;
    private long maxCustomers;
    private long users;
    private long maxUsers;
    private long dashboards;
    private long maxDashboards;
    private long edges;
    private long maxEdges;

    private long transportMessages;
    private long maxTransportMessages;
    private long jsExecutions;
    private long tbelExecutions;
    private long maxJsExecutions;
    private long maxTbelExecutions;
    private long emails;
    private long maxEmails;
    private long sms;
    private long maxSms;
    private Boolean smsEnabled;
    private long alarms;
    private long maxAlarms;

}
