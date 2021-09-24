package org.thingsboard.server.channels;

import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.TransportInfo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.slack.api.webhook.WebhookPayloads.payload;

@Component
@ConditionalOnProperty(
        value="notifications.slack.enabled",
        havingValue = "true")
public class SlackNotificationChannel implements NotificationChannel {

    @Value("${notifications.slack.webhook_url}")
    private String slackWebhookUrl;

    @Override
    public void onTransportUnavailable(TransportInfo transportInfo) {
        try {
            Slack slack = Slack.getInstance();
            String localDate = LocalDate.now().toString();
            String localTime = LocalTime.now().toString();
            String markdownTemplate = "%s | %s | *%s* | %s";

            WebhookResponse response = slack.send(slackWebhookUrl, payload(p -> p
                    .text(String.format(markdownTemplate, localDate, localTime, transportInfo.getTransportType().name(), transportInfo.getInformation()))
            ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
