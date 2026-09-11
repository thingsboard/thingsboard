// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api.notification;

import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;

public interface FirebaseService {

    void sendMessage(TenantId tenantId, String credentials, String fcmToken, String title, String body, Map<String, String> data, Integer badge) throws Exception;

}
