// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
public abstract class TbAbstractAlarmNodeConfiguration {

    static final String ALARM_DETAILS_BUILD_JS_TEMPLATE = "" +
            "var details = {};\n" +
            "if (metadata.prevAlarmDetails) {\n" +
            "    details = JSON.parse(metadata.prevAlarmDetails);\n" +
            "    //remove prevAlarmDetails from metadata\n" +
            "    delete metadata.prevAlarmDetails;\n" +
            "    //now metadata is the same as it comes IN this rule node\n" +
            "}\n" +
            "\n" +
            "\n" +
            "return details;";

    static final String ALARM_DETAILS_BUILD_TBEL_TEMPLATE = "" +
            "var details = {};\n" +
            "if (metadata.prevAlarmDetails != null) {\n" +
            "    details = JSON.parse(metadata.prevAlarmDetails);\n" +
            "    //remove prevAlarmDetails from metadata\n" +
            "    metadata.remove('prevAlarmDetails');\n" +
            "    //now metadata is the same as it comes IN this rule node\n" +
            "}\n" +
            "\n" +
            "\n" +
            "return details;";


    @NoXss
    private String alarmType;
    private ScriptLanguage scriptLang;
    private String alarmDetailsBuildJs;
    private String alarmDetailsBuildTbel;

}
