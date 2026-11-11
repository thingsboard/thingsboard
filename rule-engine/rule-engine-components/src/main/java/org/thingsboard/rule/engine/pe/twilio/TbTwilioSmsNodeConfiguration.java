// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.pe.twilio;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Created by igor on 5/25/18.
 */
@Data
public class TbTwilioSmsNodeConfiguration implements NodeConfiguration {

    private String numbersTo;
    private String numberFrom;

    private String accountSid;
    private String accountToken;

    @Override
    public NodeConfiguration defaultConfiguration() {
        TbTwilioSmsNodeConfiguration configuration = new TbTwilioSmsNodeConfiguration();
        return configuration;
    }
}
