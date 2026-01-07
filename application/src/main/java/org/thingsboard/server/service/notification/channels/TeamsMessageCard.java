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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TeamsMessageCard {
    @JsonProperty("@type")
    private final String type = "MessageCard";
    @JsonProperty("@context")
    private final String context = "http://schema.org/extensions";
    private String themeColor;
    private String summary;
    private String text;
    private List<Section> sections;
    private List<ActionCard> potentialAction;

    @Data
    public static class Section {
        private String activityTitle;
        private String activitySubtitle;
        private String activityImage;
        private List<Fact> facts;
        private boolean markdown;

        @Data
        public static class Fact {
            private final String name;
            private final String value;
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ActionCard {
        @JsonProperty("@type")
        private String type; // ActionCard, OpenUri
        private String name;
        private List<Input> inputs; // for ActionCard
        private List<Action> actions; // for ActionCard
        private List<Target> targets;

        @Data
        public static class Input {
            @JsonProperty("@type")
            private String type; // TextInput, DateInput, MultichoiceInput
            private String id;
            private boolean isMultiple;
            private String title;
            private boolean isMultiSelect;

            @Data
            public static class Choice {
                private final String display;
                private final String value;
            }
        }

        @Data
        public static class Action {
            @JsonProperty("@type")
            private final String type; // HttpPOST
            private final String name;
            private final String target; // url
        }

        @Data
        public static class Target {
            private final String os;
            private final String uri;
        }
    }

}
