package org.thingsboard.server.common.data.notification.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;
import java.util.function.Consumer;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class MicrosoftTeamsDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    private String subject;
    private String themeColor;
    private Button button;

    private String customMessageCardJson;

    public MicrosoftTeamsDeliveryMethodNotificationTemplate(DeliveryMethodNotificationTemplate other) {
        super(other);
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

    @Override
    public MicrosoftTeamsDeliveryMethodNotificationTemplate copy() {
        return new MicrosoftTeamsDeliveryMethodNotificationTemplate(this);
    }

    @Override
    public List<TemplatableValue> getTemplatableValues() {
        return List.of(
                TemplatableValue.of(body, this::setBody),
                TemplatableValue.of(subject, this::setSubject),
                TemplatableValue.of(button, Button::getName, Button::setName),
                TemplatableValue.of(button, Button::getUri, Button::setUri),
                TemplatableValue.of(customMessageCardJson, this::setCustomMessageCardJson)
        );
    }

    @Data
    public static class Button {
        private String name;
        private String uri;
    }

}
