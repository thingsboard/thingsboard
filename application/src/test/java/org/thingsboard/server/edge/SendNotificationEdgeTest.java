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
package org.thingsboard.server.edge;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.settings.SlackNotificationDeliveryMethodConfig;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.SendNotificationUplinkMsg;
import org.thingsboard.server.service.notification.EdgeNotificationRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Verifies the send-notification uplink: when the Edge delegates a Slack or mobile-push notification, the
 * Cloud receives the SendNotificationUplinkMsg, resolves the channel credentials from its own notification
 * settings and delivers. The EdgeImitator plays the Edge and sends the uplink; the Cloud's SlackService and
 * FirebaseService are mocked so we can assert the delegated call arrives with the expected arguments.
 */
@DaoSqlTest
public class SendNotificationEdgeTest extends AbstractEdgeTest {

    @MockitoBean
    private SlackService slackService;

    @MockitoBean
    private FirebaseService firebaseService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    private void prepareNotificationSettings() {
        // Slack is configured at tenant level.
        SlackNotificationDeliveryMethodConfig slack = new SlackNotificationDeliveryMethodConfig();
        slack.setBotToken("xoxb-edge-test-token");
        NotificationSettings tenantSettings = new NotificationSettings();
        Map<NotificationDeliveryMethod, NotificationDeliveryMethodConfig> tenantConfigs = new HashMap<>();
        tenantConfigs.put(NotificationDeliveryMethod.SLACK, slack);
        tenantSettings.setDeliveryMethodsConfigs(tenantConfigs);
        notificationSettingsService.saveNotificationSettings(tenantId, tenantSettings);

        // Mobile app credentials can only be configured at system level.
        MobileAppNotificationDeliveryMethodConfig mobile = new MobileAppNotificationDeliveryMethodConfig();
        mobile.setFirebaseServiceAccountCredentials("{\"type\":\"service_account\"}");
        NotificationSettings systemSettings = new NotificationSettings();
        Map<NotificationDeliveryMethod, NotificationDeliveryMethodConfig> systemConfigs = new HashMap<>();
        systemConfigs.put(NotificationDeliveryMethod.MOBILE_APP, mobile);
        systemSettings.setDeliveryMethodsConfigs(systemConfigs);
        notificationSettingsService.saveNotificationSettings(TenantId.SYS_TENANT_ID, systemSettings);
    }

    @Test
    public void testSendSlackNotificationFromEdgeProcessedByCloud() throws Exception {
        prepareNotificationSettings();
        EdgeNotificationRequest request = EdgeNotificationRequest.builder()
                .method(EdgeNotificationRequest.NotificationMethod.SEND_SLACK)
                .conversationId("C0123456789")
                .message("Edge Slack notification")
                .build();

        sendNotificationUplink(request);

        verify(slackService, timeout(30000))
                .sendMessage(eq(tenantId), anyString(), eq("C0123456789"), eq("Edge Slack notification"));
    }

    @Test
    public void testSendMobilePushFromEdgeProcessedByCloud() throws Exception {
        prepareNotificationSettings();
        EdgeNotificationRequest request = EdgeNotificationRequest.builder()
                .method(EdgeNotificationRequest.NotificationMethod.SEND_MOBILE_PUSH)
                .fcmTokens(Set.of("fcm-token-1"))
                .subject("Edge subject")
                .body("Edge body")
                .badge(3)
                .build();

        sendNotificationUplink(request);

        verify(firebaseService, timeout(30000))
                .sendMessage(eq(tenantId), anyString(), eq("fcm-token-1"), eq("Edge subject"), eq("Edge body"), any(), eq(3));
    }

    private void sendNotificationUplink(EdgeNotificationRequest request) throws Exception {
        SendNotificationUplinkMsg sendNotificationUplinkMsg = SendNotificationUplinkMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequest(JacksonUtil.toString(request))
                .build();

        sendUplinkMsgAndWaitForResponse(builder -> builder.addSendNotificationUplinkMsg(sendNotificationUplinkMsg));
    }

}
