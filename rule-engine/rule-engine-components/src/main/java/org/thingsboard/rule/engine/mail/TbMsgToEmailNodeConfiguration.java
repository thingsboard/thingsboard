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
package org.thingsboard.rule.engine.mail;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgToEmailNodeConfiguration implements NodeConfiguration<TbMsgToEmailNodeConfiguration> {

    private String fromTemplate;
    private String toTemplate;
    private String ccTemplate;
    private String bccTemplate;
    private String subjectTemplate;
    private String bodyTemplate;
    private String isHtmlTemplate;
    private String mailBodyType; // Plain Text -> false. HTML - true. Dynamic - value used from isHtmlTemplate.

    @Override
    public TbMsgToEmailNodeConfiguration defaultConfiguration() {
        var configuration = new TbMsgToEmailNodeConfiguration();
        configuration.setFromTemplate("info@testmail.org");
        configuration.setToTemplate("${userEmail}");
        configuration.setSubjectTemplate("Device ${deviceType} temperature high");
        configuration.setBodyTemplate("Device ${deviceName} has high temperature $[temperature]");
        configuration.setMailBodyType("false");
        return configuration;
    }
}
