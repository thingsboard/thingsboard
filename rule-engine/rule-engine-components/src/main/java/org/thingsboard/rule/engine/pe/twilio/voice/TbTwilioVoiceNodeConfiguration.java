// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.pe.twilio.voice;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;


@Data
public class TbTwilioVoiceNodeConfiguration implements NodeConfiguration {

    private String provider;
    private String language;
    private String voice;

    private Integer pitch;
    private Integer rate;
    private Integer volume;

    private Integer startPause;

    private String numbersTo;
    private String numberFrom;

    private String accountSid;
    private String accountToken;

    @Override
    public NodeConfiguration defaultConfiguration() {
        TbTwilioVoiceNodeConfiguration configuration = new TbTwilioVoiceNodeConfiguration();
        configuration.pitch = 100;
        configuration.rate = 100;
        configuration.volume = 0;
        return configuration;
    }
}
