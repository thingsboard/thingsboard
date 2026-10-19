// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;

public interface SmsService {

    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException;

    void sendTestSms(TenantId tenantId, TestSmsRequest testSmsRequest) throws ThingsboardException;

    boolean isConfigured(TenantId tenantId);

}
