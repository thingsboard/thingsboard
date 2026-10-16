// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import lombok.Getter;

public enum ApiUsageRecordKey {

    TRANSPORT_MSG_COUNT(ApiFeature.TRANSPORT, "transportMsgCount", "transportMsgLimit", "message"),
    TRANSPORT_DP_COUNT(ApiFeature.TRANSPORT, "transportDataPointsCount", "transportDataPointsLimit", "data point"),
    STORAGE_DP_COUNT(ApiFeature.DB, "storageDataPointsCount", "storageDataPointsLimit", "data point"),
    RE_EXEC_COUNT(ApiFeature.RE, "ruleEngineExecutionCount", "ruleEngineExecutionLimit", "Rule Engine execution"),
    JS_EXEC_COUNT(ApiFeature.JS, "jsExecutionCount", "jsExecutionLimit", "JavaScript execution"),
    TBEL_EXEC_COUNT(ApiFeature.TBEL, "tbelExecutionCount", "tbelExecutionLimit", "Tbel execution"),
    EMAIL_EXEC_COUNT(ApiFeature.EMAIL, "emailCount", "emailLimit", "email message", true, true),
    SMS_EXEC_COUNT(ApiFeature.SMS, "smsCount", "smsLimit", "SMS message", true, true),
    CREATED_ALARMS_COUNT(ApiFeature.ALARM, "createdAlarmsCount", "createdAlarmsLimit", "alarm"),
    EDGE_EVENT_COUNT(ApiFeature.EDGE, "edgeEventCount", "edgeEventLimit", "edge event"),
    ACTIVE_DEVICES("activeDevicesCount"),
    INACTIVE_DEVICES("inactiveDevicesCount"),
    GENERATED_REPORTS_COUNT(ApiFeature.REPORT, "generatedReportsCount", "generatedReportsLimit", "report"),
    INTEGRATION_MSG_COUNT(ApiFeature.INTEGRATION, "integrationMsgCount", "integrationMsgLimit", "integration message"),
    INTEGRATION_DP_COUNT(ApiFeature.INTEGRATION, "integrationDataPointsCount", "integrationDataPointsLimit", "data point"),
    AI_CREDITS_COUNT(ApiFeature.AI, "aiCreditsCount", "aiCreditsLimit", "AI credit", true, true);

    private static final ApiUsageRecordKey[] JS_RECORD_KEYS = {JS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] TBEL_RECORD_KEYS = {TBEL_EXEC_COUNT};
    private static final ApiUsageRecordKey[] RE_RECORD_KEYS = {RE_EXEC_COUNT};
    private static final ApiUsageRecordKey[] DB_RECORD_KEYS = {STORAGE_DP_COUNT};
    private static final ApiUsageRecordKey[] TRANSPORT_RECORD_KEYS = {TRANSPORT_MSG_COUNT, TRANSPORT_DP_COUNT};
    private static final ApiUsageRecordKey[] EMAIL_RECORD_KEYS = {EMAIL_EXEC_COUNT};
    private static final ApiUsageRecordKey[] SMS_RECORD_KEYS = {SMS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] ALARM_RECORD_KEYS = {CREATED_ALARMS_COUNT};
    private static final ApiUsageRecordKey[] EDGE_RECORD_KEYS = {EDGE_EVENT_COUNT};
    private static final ApiUsageRecordKey[] REPORT_RECORD_KEYS = {GENERATED_REPORTS_COUNT};
    private static final ApiUsageRecordKey[] INTEGRATION_RECORD_KEYS = {INTEGRATION_MSG_COUNT, INTEGRATION_DP_COUNT};
    private static final ApiUsageRecordKey[] AI_RECORD_KEYS = {AI_CREDITS_COUNT};

    @Getter
    private final ApiFeature apiFeature;
    @Getter
    private final String apiCountKey;
    @Getter
    private final String apiLimitKey;
    @Getter
    private final String unitLabel;
    @Getter
    private final boolean counter;
    @Getter
    private final boolean urgent; // urgent keys are reported at a shorter interval for quicker usage state updates

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel) {
        this(apiFeature, apiCountKey, apiLimitKey, unitLabel, true, false);
    }

    ApiUsageRecordKey(String apiCountKey) {
        this(null, apiCountKey, null, null, false, false);
    }

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel, boolean counter, boolean urgent) {
        this.apiFeature = apiFeature;
        this.apiCountKey = apiCountKey;
        this.apiLimitKey = apiLimitKey;
        this.unitLabel = unitLabel;
        this.counter = counter;
        this.urgent = urgent;
    }

    public static ApiUsageRecordKey[] getKeys(ApiFeature feature) {
        return switch (feature) {
            case TRANSPORT -> TRANSPORT_RECORD_KEYS;
            case DB -> DB_RECORD_KEYS;
            case RE -> RE_RECORD_KEYS;
            case JS -> JS_RECORD_KEYS;
            case TBEL -> TBEL_RECORD_KEYS;
            case EMAIL -> EMAIL_RECORD_KEYS;
            case SMS -> SMS_RECORD_KEYS;
            case ALARM -> ALARM_RECORD_KEYS;
            case REPORT -> REPORT_RECORD_KEYS;
            case EDGE -> EDGE_RECORD_KEYS;
            case INTEGRATION -> INTEGRATION_RECORD_KEYS;
            case AI -> AI_RECORD_KEYS;
        };
    }

}
