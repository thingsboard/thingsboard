// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.script.api.js;

/**
 * Created by igor on 5/24/18.
 */
public class DownlinkConverterScriptFactory {

    private static final String JS_HELPERS_PREFIX_TEMPLATE = "load('classpath:js/converter-helpers.js'); ";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(msgStr, metadataStr, msgType, integrationMetadataStr) { " +
            "    var msg = JSON.parse(msgStr); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    var integrationMetadata = JSON.parse(integrationMetadataStr); " +
            "    return JSON.stringify(Encoder(msg, metadata, msgType, integrationMetadata));" +
            "    function Encoder(msg, metadata, msgType, integrationMetadata) {";

    private static final String JS_WRAPPER_SUFFIX = "}\n}";

    public static String generateDownlinkConverterScript(String functionName, String scriptBody, boolean isLocal) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        String result = jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
        if (isLocal) {
            result = JS_HELPERS_PREFIX_TEMPLATE + result;
        }
        return result;
    }
}
