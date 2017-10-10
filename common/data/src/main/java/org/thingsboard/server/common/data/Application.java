/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.ApplicationId;
import java.util.List;

public class Application extends SearchTextBased<ApplicationId> implements HasName {

    private static final long serialVersionUID = 1533755382827939542L;

    private JsonNode miniWidget;
    private JsonNode dashboard;
    private JsonNode rules;
    private String name;
    private String description;
    private List<String> deviceTypes;

    public Application() {
        super();
    }

    public Application(ApplicationId id) {
        super(id);
    }

    public Application(Application application) {
        super(application);
        this.miniWidget = application.miniWidget;
        this.dashboard = application.dashboard;
        this.rules = application.rules;
        this.name = application.name;
        this.description = application.description;
        this.deviceTypes = application.deviceTypes;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSearchText() {
        return name;
    }

    public JsonNode getMiniWidget() {
        return miniWidget;
    }

    public void setMiniWidget(JsonNode miniWidget) {
        this.miniWidget = miniWidget;
    }

    public JsonNode getDashboard() {
        return dashboard;
    }

    public void setDashboard(JsonNode dashboard) {
        this.dashboard = dashboard;
    }

    public JsonNode getRules() {
        return rules;
    }

    public void setRules(JsonNode rules) {
        this.rules = rules;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDeviceTypes() {
        return deviceTypes;
    }

    public void setDeviceTypes(List<String> deviceTypes) {
        this.deviceTypes = deviceTypes;
    }


    @Override
    public String toString() {
        return "Application{" +
                "miniWidget=" + miniWidget +
                ", dashboard=" + dashboard +
                ", rules=" + rules +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", deviceTypes=" + deviceTypes +
                '}';
    }
}
