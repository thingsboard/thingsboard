/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.notification.channels;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.template.NotificationText;
import org.thingsboard.server.service.sms.SmsExecutorService;

@Component
@RequiredArgsConstructor
public class SmsNotificationChannel implements NotificationChannel {

    private final SmsService smsService;
    private final SmsExecutorService executor;

    @Override
    public ListenableFuture<Void> sendNotification(User recipient, NotificationRequest request, NotificationText text) {
        String phone = recipient.getPhone();
        if (StringUtils.isBlank(phone)) return Futures.immediateFailedFuture(new RuntimeException("User does not have phone number"));
        return executor.submit(() -> {
            smsService.sendSms(recipient.getTenantId(), recipient.getCustomerId(), new String[]{phone}, text.getBody());
            return null;
        });
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.SMS;
    }

}
