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
package org.thingsboard.rule.engine.notification;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TbNotificationNodeConfiguration implements NodeConfiguration<TbNotificationNodeConfiguration> {

    @NotEmpty
    private List<NotificationTargetId> targets;
    @NotNull
    private NotificationTemplateId templateId;
    private NotificationRequestConfig additionalConfig;

    @Override
    public TbNotificationNodeConfiguration defaultConfiguration() {
        TbNotificationNodeConfiguration config = new TbNotificationNodeConfiguration();
        config.setAdditionalConfig(new NotificationRequestConfig());
        return config;
    }

}
