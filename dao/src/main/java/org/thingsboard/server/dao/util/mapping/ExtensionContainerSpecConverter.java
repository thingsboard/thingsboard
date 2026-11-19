// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.util.mapping;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;

@Converter
public class ExtensionContainerSpecConverter implements AttributeConverter<ExtensionContainerSpec, String> {

    @Override
    public String convertToDatabaseColumn(ExtensionContainerSpec containerSpec) {
        return JacksonUtil.toString(containerSpec);
    }

    @Override
    public ExtensionContainerSpec convertToEntityAttribute(String json) {
        return JacksonUtil.fromString(json, ExtensionContainerSpec.class);
    }

}
