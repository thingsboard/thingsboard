// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util.mapping;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItemDescriptor;

@Converter
public class IotHubInstalledItemDescriptorConverter implements AttributeConverter<IotHubInstalledItemDescriptor, String> {

    @Override
    public String convertToDatabaseColumn(IotHubInstalledItemDescriptor descriptor) {
        return JacksonUtil.toString(descriptor);
    }

    @Override
    public IotHubInstalledItemDescriptor convertToEntityAttribute(String json) {
        return JacksonUtil.fromString(json, IotHubInstalledItemDescriptor.class);
    }

}
