/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
public class TbSendEmailNodeConfiguration implements NodeConfiguration {

    private boolean useSystemSmtpSettings;
    private String smtpHost;
    private int smtpPort;
    private String username;
    private String password;
    private String smtpProtocol;
    private int timeout;
    private boolean enableTls;
    private String tlsVersion;
    private boolean enableProxy;
    private String proxyHost;
    private String proxyPort;
    private String proxyUser;
    private String proxyPassword;

    @Override
    public TbSendEmailNodeConfiguration defaultConfiguration() {
        TbSendEmailNodeConfiguration configuration = new TbSendEmailNodeConfiguration();
        configuration.setUseSystemSmtpSettings(true);
        configuration.setSmtpHost("localhost");
        configuration.setSmtpProtocol("smtp");
        configuration.setSmtpPort(25);
        configuration.setTimeout(10000);
        configuration.setEnableTls(false);
        configuration.setTlsVersion("TLSv1.2");
        configuration.setEnableProxy(false);
        return configuration;
    }
}
