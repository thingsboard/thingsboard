// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.prototypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.common.util.JacksonUtil;

public class HttpIntegrationConfigPrototypes {

    private static final String CONFIG = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":false," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"headersFilter\":{}," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"headersFilter\":{}," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED_WITH_TEST_HEADER = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"headersFilter\": {\"testHeader\": \"testValue\"}," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    private static final String CONFIG_SECURITY_ENABLED_TEST_HEADER2 = " {\"baseUrl\":\"%s\"," +
            "\"replaceNoContentToOk\":true," +
            "\"enableSecurity\":true," +
            "\"headersFilter\": {\"testHeader2\": \"testValue2\"}," +
            "\"downlinkUrl\":\"https://api.thingpark.com/thingpark/lrc/rest/downlink\"," +
            "\"loriotDownlinkUrl\":\"https://eu1.loriot.io/1/rest\"," +
            "\"createLoriotOutput\":false," +
            "\"sendDownlink\":false," +
            "\"server\":\"eu1\"," +
            "\"appId\":\"\"," +
            "\"enableSecurityNew\":false," +
            "\"asId\":\"\"," +
            "\"asIdNew\":\"\"," +
            "\"asKey\":\"\"," +
            "\"clientIdNew\":\"\"," +
            "\"clientSecret\":\"\"," +
            "\"maxTimeDiffInSeconds\":60," +
            "\"httpEndpoint\":\"\"," +
            "\"token\":\"\"," +
            "\"credentials\":{\"type\":\"basic\",\"email\":\"\",\"password\":\"\",\"token\":\"\"}," +
            "\"metadata\":{}}";

    public static JsonNode defaultConfig(String httpsUrl){
        return JacksonUtil.toJsonNode(String.format(CONFIG, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityEnabled(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityHeader(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED_WITH_TEST_HEADER, httpsUrl));
    }

    public static JsonNode defaultConfigWithSecurityHeader2(String httpsUrl) {
        return JacksonUtil.toJsonNode(String.format(CONFIG_SECURITY_ENABLED_TEST_HEADER2, httpsUrl));
    }
}
