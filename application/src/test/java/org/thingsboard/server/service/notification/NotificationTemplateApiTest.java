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
package org.thingsboard.server.service.notification;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class NotificationTemplateApiTest extends AbstractControllerTest {

    @Before
    public void beforeEach() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void givenInvalidNotificationTemplate_whenSaving_returnValidationError() throws Exception {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setTenantId(tenantId);
        notificationTemplate.setName(null);
        notificationTemplate.setNotificationType(null);
        notificationTemplate.setNotificationSubject(null);
        notificationTemplate.setConfiguration(null);

        String validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("name must not be")
                .contains("notificationType must not be")
                .contains("configuration must not be");

        NotificationTemplateConfig config = new NotificationTemplateConfig();
        notificationTemplate.setConfiguration(config);
        config.setDefaultTextTemplate("Default text");
        EmailDeliveryMethodNotificationTemplate emailTemplate = new EmailDeliveryMethodNotificationTemplate();
        emailTemplate.setMethod(NotificationDeliveryMethod.EMAIL);
        emailTemplate.setBody(null);
        emailTemplate.setSubject(null);
        config.setTemplates(Map.of(
                NotificationDeliveryMethod.EMAIL, emailTemplate
        ));
        notificationTemplate.setName("<script/>");

        validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .doesNotContain("defaultTextTemplate must be specified")
                .contains("name is malformed");

        config.setDefaultTextTemplate(null);

        validationError = saveAndGetError(notificationTemplate, status().isBadRequest());
        assertThat(validationError)
                .contains("defaultTextTemplate must be specified");
    }

    private String saveAndGetError(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return getErrorMessage(save(notificationTemplate, statusMatcher));
    }

    private ResultActions save(NotificationTemplate notificationTemplate, ResultMatcher statusMatcher) throws Exception {
        return doPost("/api/notification/template", notificationTemplate)
                .andExpect(statusMatcher);
    }

}
