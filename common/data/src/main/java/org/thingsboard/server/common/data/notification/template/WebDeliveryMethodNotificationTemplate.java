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
package org.thingsboard.server.common.data.notification.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WebDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    @NoXss(fieldName = "web notification subject")
    @Length(fieldName = "web notification subject", max = 150, message = "cannot be longer than 150 chars")
    @NotEmpty
    private String subject;
    private JsonNode additionalConfig;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject),
            TemplatableValue.of(this::getButtonText, this::setButtonText),
            TemplatableValue.of(this::getButtonLink, this::setButtonLink)
    );

    public WebDeliveryMethodNotificationTemplate(WebDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.additionalConfig = other.additionalConfig != null ? other.additionalConfig.deepCopy() : null;
    }

    @Length(fieldName = "web notification message", max = 250, message = "cannot be longer than 250 chars")
    @Override
    public String getBody() {
        return super.getBody();
    }

    @NoXss(fieldName = "web notification button text")
    @Length(fieldName = "web notification button text", max = 50, message = "cannot be longer than 50 chars")
    @JsonIgnore
    public String getButtonText() {
        return getButtonConfigProperty("text");
    }

    @JsonIgnore
    public void setButtonText(String buttonText) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("text", new TextNode(buttonText));
        });
    }

    @NoXss(fieldName = "web notification button link")
    @Length(fieldName = "web notification button link", max = 300, message = "cannot be longer than 300 chars")
    @JsonIgnore
    public String getButtonLink() {
        return getButtonConfigProperty("link");
    }

    @JsonIgnore
    public void setButtonLink(String buttonLink) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("link", new TextNode(buttonLink));
        });
    }

    private String getButtonConfigProperty(String property) {
        return getButtonConfig()
                .map(buttonConfig -> buttonConfig.get(property))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    private Optional<ObjectNode> getButtonConfig() {
        return Optional.ofNullable(additionalConfig)
                .map(config -> config.get("actionButtonConfig")).filter(JsonNode::isObject)
                .map(config -> (ObjectNode) config);
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.WEB;
    }

    @Override
    public WebDeliveryMethodNotificationTemplate copy() {
        return new WebDeliveryMethodNotificationTemplate(this);
    }

}
