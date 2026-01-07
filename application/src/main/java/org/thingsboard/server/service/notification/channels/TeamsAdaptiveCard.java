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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.dao.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @link <a href="https://adaptivecards.io/designer/">AdaptiveCard Designer</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamsAdaptiveCard {
    private String type = "message";
    private List<Attachment> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attachment {
        private String contentType = "application/vnd.microsoft.card.adaptive";
        private AdaptiveCard content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdaptiveCard {
        @JsonProperty("$schema")
        private final String schema = "http://adaptivecards.io/schemas/adaptive-card.json";
        private final String type = "AdaptiveCard";
        private BackgroundImage backgroundImage;
        @JsonProperty("body")
        private List<TextBlock> textBlocks = new ArrayList<>();
        private List<ActionOpenUrl> actions = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class BackgroundImage {
        private String url;
        private final String fillMode = "repeat";

        public BackgroundImage(String color) {
            // This is the only one way how to specify color the custom color for the card
            url = ImageUtils.getEmbeddedBase64EncodedImg(color);
        }

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextBlock {
        private final String type = "TextBlock";
        private String text;
        private String weight = "Normal";
        private String size = "Medium";
        private String spacing = "None";
        private String color = "#FFFFFF";
        private final boolean wrap = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionOpenUrl {
        private final String type = "Action.OpenUrl";
        private String title;
        private String url;
    }

}