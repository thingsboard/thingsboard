// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
