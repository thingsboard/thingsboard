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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate.Button.LinkType;
import org.thingsboard.server.service.notification.NotificationProcessingContext;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MicrosoftTeamsNotificationChannel implements NotificationChannel<MicrosoftTeamsNotificationTargetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate> {

    private final SystemSecurityService systemSecurityService;

    @Setter
    private RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .build();

    @Override
    public void sendNotification(MicrosoftTeamsNotificationTargetConfig targetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        if (targetConfig.getUseOldApi() == null || Boolean.TRUE.equals(targetConfig.getUseOldApi())) {
            sendTeamsMessageCard(targetConfig, processedTemplate, ctx);
        } else {
            sendTeamsAdaptiveCard(targetConfig, processedTemplate, ctx);
        }
    }

    private void sendTeamsAdaptiveCard(MicrosoftTeamsNotificationTargetConfig targetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws URISyntaxException, JsonProcessingException {
        TeamsAdaptiveCard teamsAdaptiveCard = new TeamsAdaptiveCard();
        TeamsAdaptiveCard.Attachment attachment = new TeamsAdaptiveCard.Attachment();
        teamsAdaptiveCard.setAttachments(List.of(attachment));
        TeamsAdaptiveCard.AdaptiveCard adaptiveCard = new TeamsAdaptiveCard.AdaptiveCard();
        attachment.setContent(adaptiveCard);
        TeamsAdaptiveCard.BackgroundImage backgroundImage = new TeamsAdaptiveCard.BackgroundImage(processedTemplate.getThemeColor());
        adaptiveCard.setBackgroundImage(backgroundImage);

        if (StringUtils.isEmpty(processedTemplate.getSubject())) {
            TeamsAdaptiveCard.TextBlock textBlock = new TeamsAdaptiveCard.TextBlock();
            textBlock.setText(processedTemplate.getBody());
            textBlock.setWeight("Normal");
            textBlock.setSize("Medium");
            textBlock.setColor(processedTemplate.getThemeColor());
            adaptiveCard.getTextBlocks().add(textBlock);
        } else {
            TeamsAdaptiveCard.TextBlock subjectTextBlock = new TeamsAdaptiveCard.TextBlock();
            subjectTextBlock.setText(processedTemplate.getSubject());
            subjectTextBlock.setWeight("Bolder");
            subjectTextBlock.setSize("Large");
            subjectTextBlock.setColor(processedTemplate.getThemeColor());

            adaptiveCard.getTextBlocks().add(subjectTextBlock);

            TeamsAdaptiveCard.TextBlock bodyTextBlock = new TeamsAdaptiveCard.TextBlock();
            bodyTextBlock.setText(processedTemplate.getBody());
            bodyTextBlock.setWeight("Lighter");
            bodyTextBlock.setSize("Medium");
            bodyTextBlock.setColor(processedTemplate.getThemeColor());

            adaptiveCard.getTextBlocks().add(bodyTextBlock);
        }

        String uri = getButtonUri(processedTemplate, ctx);

        if (StringUtils.isNotBlank(uri) && processedTemplate.getButton().getText() != null) {
            TeamsAdaptiveCard.ActionOpenUrl actionOpenUrl = new TeamsAdaptiveCard.ActionOpenUrl();
            actionOpenUrl.setTitle(processedTemplate.getButton().getText());
            actionOpenUrl.setUrl(uri);
            adaptiveCard.getActions().add(actionOpenUrl);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(JacksonUtil.toString(teamsAdaptiveCard), headers);
        restTemplate.postForEntity(new URI(targetConfig.getWebhookUrl()), request, String.class);
    }

    private void sendTeamsMessageCard(MicrosoftTeamsNotificationTargetConfig targetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws JsonProcessingException, URISyntaxException {
        TeamsMessageCard teamsMessageCard = new TeamsMessageCard();
        teamsMessageCard.setThemeColor(Strings.emptyToNull(processedTemplate.getThemeColor()));
        if (StringUtils.isEmpty(processedTemplate.getSubject())) {
            teamsMessageCard.setText(processedTemplate.getBody());
        } else {
            teamsMessageCard.setSummary(processedTemplate.getSubject());
            TeamsMessageCard.Section section = new TeamsMessageCard.Section();
            section.setActivityTitle(processedTemplate.getSubject());
            section.setActivitySubtitle(processedTemplate.getBody());
            teamsMessageCard.setSections(List.of(section));
        }
        var button = processedTemplate.getButton();
        String uri = getButtonUri(processedTemplate, ctx);

        if (StringUtils.isNotBlank(uri) && button.getText() != null) {
            TeamsMessageCard.ActionCard actionCard = new TeamsMessageCard.ActionCard();
            actionCard.setType("OpenUri");
            actionCard.setName(button.getText());
            var target = new TeamsMessageCard.ActionCard.Target("default", uri);
            actionCard.setTargets(List.of(target));
            teamsMessageCard.setPotentialAction(List.of(actionCard));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(JacksonUtil.toString(teamsMessageCard), headers);
        restTemplate.postForEntity(new URI(targetConfig.getWebhookUrl()), request, String.class);
    }

    private String getButtonUri(MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws JsonProcessingException {
        var button = processedTemplate.getButton();
        if (button != null && button.isEnabled()) {
            String uri;
            if (button.getLinkType() == LinkType.DASHBOARD) {
                String state = null;
                if (button.isSetEntityIdInState() || StringUtils.isNotEmpty(button.getDashboardState())) {
                    ObjectNode stateObject = JacksonUtil.newObjectNode();
                    if (button.isSetEntityIdInState()) {
                        stateObject.putObject("params")
                                .set("entityId", Optional.ofNullable(ctx.getRequest().getInfo())
                                        .map(NotificationInfo::getStateEntityId)
                                        .map(JacksonUtil::valueToTree)
                                        .orElse(null));
                    } else {
                        stateObject.putObject("params");
                    }
                    if (StringUtils.isNotEmpty(button.getDashboardState())) {
                        stateObject.put("id", button.getDashboardState());
                    }
                    state = Base64.encodeBase64String(JacksonUtil.OBJECT_MAPPER.writeValueAsBytes(List.of(stateObject)));
                }
                String baseUrl = systemSecurityService.getBaseUrl(ctx.getTenantId(), null, null);
                if (StringUtils.isEmpty(baseUrl)) {
                    throw new IllegalStateException("Failed to determine base url to construct dashboard link");
                }
                uri = baseUrl + "/dashboards/" + button.getDashboardId();
                if (state != null) {
                    uri += "?state=" + state;
                }
            } else {
                uri = button.getLink();
            }
            return uri;
        }
        return null;
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

}
