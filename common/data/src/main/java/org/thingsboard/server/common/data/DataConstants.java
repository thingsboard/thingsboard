/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

/**
 * @author Andrew Shvayka
 */
public class DataConstants {

    public static final String TENANT = "TENANT";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DEVICE = "DEVICE";

    public static final String SCOPE = "scope";
    public static final String CLIENT_SCOPE = "CLIENT_SCOPE";
    public static final String SERVER_SCOPE = "SERVER_SCOPE";
    public static final String SHARED_SCOPE = "SHARED_SCOPE";
    public static final String NOTIFY_DEVICE_METADATA_KEY = "notifyDevice";
    public static final String LATEST_TS = "LATEST_TS";
    public static final String IS_NEW_ALARM = "isNewAlarm";
    public static final String IS_EXISTING_ALARM = "isExistingAlarm";
    public static final String IS_SEVERITY_UPDATED_ALARM = "isSeverityUpdated";
    public static final String IS_CLEARED_ALARM = "isClearedAlarm";
    public static final String ALARM_CONDITION_REPEATS = "alarmConditionRepeats";
    public static final String ALARM_CONDITION_DURATION = "alarmConditionDuration";
    public static final String PERSISTENT = "persistent";
    public static final String TIMEOUT = "timeout";
    public static final String EXPIRATION_TIME = "expirationTime";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    public static final String RETRIES = "retries";
    public static final String EDGE_ID = "edgeId";
    public static final String DEVICE_ID = "deviceId";
    public static final String COAP_TRANSPORT_NAME = "COAP";
    public static final String LWM2M_TRANSPORT_NAME = "LWM2M";
    public static final String MQTT_TRANSPORT_NAME = "MQTT";
    public static final String HTTP_TRANSPORT_NAME = "HTTP";
    public static final String SNMP_TRANSPORT_NAME = "SNMP";


    public static final String[] allScopes() {
        return new String[]{CLIENT_SCOPE, SHARED_SCOPE, SERVER_SCOPE};
    }

    public static final String IN = "IN";
    public static final String OUT = "OUT";

    public static final String DEFAULT_SECRET_KEY = "";
    public static final String SECRET_KEY_FIELD_NAME = "secretKey";
    public static final String DURATION_MS_FIELD_NAME = "durationMs";

    public static final String PROVISION = "provision";
    public static final String PROVISION_KEY = "provisionDeviceKey";
    public static final String PROVISION_SECRET = "provisionDeviceSecret";

    public static final String DEFAULT_DEVICE_TYPE = "default";
    public static final String DEVICE_NAME = "deviceName";
    public static final String DEVICE_TYPE = "deviceType";
    public static final String CERT_PUB_KEY = "x509CertPubKey";
    public static final String CREDENTIALS_TYPE = "credentialsType";
    public static final String TOKEN = "token";
    public static final String HASH = "hash";
    public static final String CLIENT_ID = "clientId";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EDGE_MSG_SOURCE = "edge";
    public static final String MSG_SOURCE_KEY = "source";

    public static final String LAST_CONNECTED_GATEWAY = "lastConnectedGateway";

    public static final String MAIN_QUEUE_NAME = "Main";
    public static final String MAIN_QUEUE_TOPIC = "tb_rule_engine.main";
    public static final String HP_QUEUE_NAME = "HighPriority";
    public static final String HP_QUEUE_TOPIC = "tb_rule_engine.hp";
    public static final String SQ_QUEUE_NAME = "SequentialByOriginator";
    public static final String SQ_QUEUE_TOPIC = "tb_rule_engine.sq";

}
