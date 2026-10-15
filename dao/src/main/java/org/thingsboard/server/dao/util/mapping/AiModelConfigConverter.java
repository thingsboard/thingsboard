// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util.mapping;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ai.model.AiModelConfig;

@Converter
public class AiModelConfigConverter implements AttributeConverter<AiModelConfig, String> {

    @Override
    public String convertToDatabaseColumn(AiModelConfig config) {
        return JacksonUtil.toString(config);
    }

    @Override
    public AiModelConfig convertToEntityAttribute(String json) {
        return JacksonUtil.fromString(json, AiModelConfig.class);
    }

}
