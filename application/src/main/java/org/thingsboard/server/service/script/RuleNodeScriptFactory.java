/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thingsboard.server.service.script;

public class RuleNodeScriptFactory {

    public static final String MSG = "msg";
    public static final String METADATA = "metadata";
    public static final String MSG_TYPE = "msgType";
    public static final String RULE_NODE_FUNCTION_NAME = "ruleNodeFunc";

    private static final String JS_WRAPPER_PREFIX_TEMPLATE = "function %s(msgStr, metadataStr, msgType) { " +
            "    var msg = JSON.parse(msgStr); " +
            "    var metadata = JSON.parse(metadataStr); " +
            "    return JSON.stringify(%s(msg, metadata, msgType));" +
            "    function %s(%s, %s, %s) {";
    private static final String JS_WRAPPER_SUFFIX = "}" +
            "\n}";


    public static String generateRuleNodeScript(String functionName, String scriptBody, String... argNames) {
        String msgArg;
        String metadataArg;
        String msgTypeArg;
        if (argNames != null && argNames.length == 3) {
            msgArg = argNames[0];
            metadataArg = argNames[1];
            msgTypeArg = argNames[2];
        } else {
            msgArg = MSG;
            metadataArg = METADATA;
            msgTypeArg = MSG_TYPE;
        }
        String jsWrapperPrefix = String.format(JS_WRAPPER_PREFIX_TEMPLATE, functionName,
                RULE_NODE_FUNCTION_NAME, RULE_NODE_FUNCTION_NAME, msgArg, metadataArg, msgTypeArg);
        return jsWrapperPrefix + scriptBody + JS_WRAPPER_SUFFIX;
    }

}
