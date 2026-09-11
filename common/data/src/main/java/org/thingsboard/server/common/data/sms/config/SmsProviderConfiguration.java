// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sms.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AwsSnsSmsProviderConfiguration.class, name = "AWS_SNS"),
        @JsonSubTypes.Type(value = TwilioSmsProviderConfiguration.class, name = "TWILIO"),
        @JsonSubTypes.Type(value = SmppSmsProviderConfiguration.class, name = "SMPP")
})
public interface SmsProviderConfiguration {

    @JsonIgnore
    SmsProviderType getType();

}
