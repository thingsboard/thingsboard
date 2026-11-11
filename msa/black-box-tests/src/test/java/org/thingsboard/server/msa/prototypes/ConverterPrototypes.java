// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.integration.IntegrationType;

public class ConverterPrototypes {

    public static Converter uplinkConverterPrototype(JsonNode config) {
        return uplinkConverterPrototype(config, null, null);
    }

    public static Converter uplinkConverterPrototype(JsonNode config, IntegrationType integrationType, Integer converterVersion) {
        Converter converter = new Converter();
        converter.setName("Uplink converter " + StringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(config);
        converter.setDebugSettings(DebugSettings.all());
        converter.setConverterVersion(converterVersion);
        converter.setIntegrationType(integrationType);
        return converter;
    }

    public static Converter downlinkConverterPrototype(JsonNode config){
        Converter converter = new Converter();
        converter.setName("Downlink converter " + StringUtils.randomAlphanumeric(7));
        converter.setType(ConverterType.DOWNLINK);
        converter.setConfiguration(config);
        converter.setDebugSettings(DebugSettings.all());
        return converter;
    }
}
