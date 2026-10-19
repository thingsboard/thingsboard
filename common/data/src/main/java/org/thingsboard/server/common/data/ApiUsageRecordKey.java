// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import lombok.Getter;

public enum ApiUsageRecordKey {

    TRANSPORT_MSG_COUNT(ApiFeature.TRANSPORT, "transportMsgCount", "transportMsgLimit", "message"),
    TRANSPORT_DP_COUNT(ApiFeature.TRANSPORT, "transportDataPointsCount", "transportDataPointsLimit", "data point"),
    STORAGE_DP_COUNT(ApiFeature.DB, "storageDataPointsCount", "storageDataPointsLimit", "data point"),
    RE_EXEC_COUNT(ApiFeature.RE, "ruleEngineExecutionCount", "ruleEngineExecutionLimit", "Rule Engine execution"),
    JS_EXEC_COUNT(ApiFeature.JS, "jsExecutionCount", "jsExecutionLimit", "JavaScript execution"),
    TBEL_EXEC_COUNT(ApiFeature.TBEL, "tbelExecutionCount", "tbelExecutionLimit", "Tbel execution"),
    EMAIL_EXEC_COUNT(ApiFeature.EMAIL, "emailCount", "emailLimit", "email message"),
    SMS_EXEC_COUNT(ApiFeature.SMS, "smsCount", "smsLimit", "SMS message"),
    CREATED_ALARMS_COUNT(ApiFeature.ALARM, "createdAlarmsCount", "createdAlarmsLimit", "alarm"),
    ACTIVE_DEVICES("activeDevicesCount"),
    INACTIVE_DEVICES("inactiveDevicesCount");

    private static final ApiUsageRecordKey[] JS_RECORD_KEYS = {JS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] TBEL_RECORD_KEYS = {TBEL_EXEC_COUNT};
    private static final ApiUsageRecordKey[] RE_RECORD_KEYS = {RE_EXEC_COUNT};
    private static final ApiUsageRecordKey[] DB_RECORD_KEYS = {STORAGE_DP_COUNT};
    private static final ApiUsageRecordKey[] TRANSPORT_RECORD_KEYS = {TRANSPORT_MSG_COUNT, TRANSPORT_DP_COUNT};
    private static final ApiUsageRecordKey[] EMAIL_RECORD_KEYS = {EMAIL_EXEC_COUNT};
    private static final ApiUsageRecordKey[] SMS_RECORD_KEYS = {SMS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] ALARM_RECORD_KEYS = {CREATED_ALARMS_COUNT};

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

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel) {
        this(apiFeature, apiCountKey, apiLimitKey, unitLabel, true);
    }

    ApiUsageRecordKey(String apiCountKey) {
        this(null, apiCountKey, null, null, false);
    }

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel, boolean counter) {
        this.apiFeature = apiFeature;
        this.apiCountKey = apiCountKey;
        this.apiLimitKey = apiLimitKey;
        this.unitLabel = unitLabel;
        this.counter = counter;
    }

    public static ApiUsageRecordKey[] getKeys(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return TRANSPORT_RECORD_KEYS;
            case DB:
                return DB_RECORD_KEYS;
            case RE:
                return RE_RECORD_KEYS;
            case JS:
                return JS_RECORD_KEYS;
            case TBEL:
                return TBEL_RECORD_KEYS;
            case EMAIL:
                return EMAIL_RECORD_KEYS;
            case SMS:
                return SMS_RECORD_KEYS;
            case ALARM:
                return ALARM_RECORD_KEYS;
            default:
                return new ApiUsageRecordKey[]{};
        }
    }

}
