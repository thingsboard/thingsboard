// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.script.api.js;

public class AttributesScriptFactory {

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(attributesStr) { " +
            "    var attributes = JSON.parse(attributesStr); " +
            "    return JSON.stringify(attributesFunc(attributes));" +
            "    function attributesFunc(attributes) {";
    private static final String JS_WRAPPER_SUFFIX = "}" +
            "\n}";

    public static String generateAttributesScript(String functionName, String scriptBody) {
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName);
        return jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
    }

}
