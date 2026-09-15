// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.adaptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class JsonConverterConfig {

    @Value("${transport.json.type_cast_enabled:true}")
    public void setIsJsonTypeCastEnabled(boolean jsonTypeCastEnabled) {
        JsonConverter.setTypeCastEnabled(jsonTypeCastEnabled);
        log.info("JSON type cast enabled = {}", jsonTypeCastEnabled);
    }

    @Value("${transport.json.max_string_value_length:0}")
    public void setMaxStringValueLength(int maxStringValueLength) {
        JsonConverter.setMaxStringValueLength(maxStringValueLength);
        log.info("JSON max string value length = {}", maxStringValueLength);
    }
}
