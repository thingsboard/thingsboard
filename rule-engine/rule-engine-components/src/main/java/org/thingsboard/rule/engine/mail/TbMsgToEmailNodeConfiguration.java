// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
