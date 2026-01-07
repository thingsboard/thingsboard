/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

/**
 * Created by ashvayka on 19.01.17.
 */
public class MqttTopics {

    private static final String REQUEST = "/request";
    private static final String RESPONSE = "/response";
    private static final String RPC = "/rpc";
    private static final String CONNECT = "/connect";
    private static final String DISCONNECT = "/disconnect";
    private static final String TELEMETRY = "/telemetry";
    private static final String ATTRIBUTES = "/attributes";
    private static final String CLAIM = "/claim";
    private static final String SUB_TOPIC = "+";
    private static final String PROVISION = "/provision";
    private static final String FIRMWARE = "/fw";
    private static final String SOFTWARE = "/sw";
    private static final String CHUNK = "/chunk/";
    private static final String ERROR = "/error";
    private static final String TELEMETRY_SHORT = "/t";
    private static final String ATTRIBUTES_SHORT = "/a";
    private static final String RPC_SHORT = "/r";
    private static final String REQUEST_SHORT = "/req";
    private static final String RESPONSE_SHORT = "/res";
    private static final String JSON_SHORT = "j";
    private static final String PROTO_SHORT = "p";
    private static final String ATTRIBUTES_RESPONSE = ATTRIBUTES + RESPONSE;
    private static final String ATTRIBUTES_REQUEST = ATTRIBUTES + REQUEST;
    private static final String ATTRIBUTES_RESPONSE_SHORT = ATTRIBUTES_SHORT + RESPONSE_SHORT + "/";
    private static final String ATTRIBUTES_REQUEST_SHORT = ATTRIBUTES_SHORT + REQUEST_SHORT + "/";
    private static final String DEVICE_RPC_RESPONSE = RPC + RESPONSE + "/";
    private static final String DEVICE_RPC_REQUEST = RPC + REQUEST + "/";
    private static final String DEVICE_RPC_RESPONSE_SHORT = RPC_SHORT + RESPONSE_SHORT + "/";
    private static final String DEVICE_RPC_REQUEST_SHORT = RPC_SHORT + REQUEST_SHORT + "/";
    private static final String DEVICE_ATTRIBUTES_RESPONSE = ATTRIBUTES_RESPONSE + "/";
    private static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";
    // v1 topics
    public static final String BASE_DEVICE_API_TOPIC = "v1/devices/me";
    public static final String DEVICE_RPC_RESPONSE_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_RESPONSE;
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC = DEVICE_RPC_RESPONSE_TOPIC + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_REQUEST;
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC = DEVICE_RPC_REQUESTS_TOPIC + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_RESPONSE;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_REQUEST;
    public static final String DEVICE_TELEMETRY_TOPIC = BASE_DEVICE_API_TOPIC + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC = BASE_DEVICE_API_TOPIC + CLAIM;
    public static final String DEVICE_ATTRIBUTES_TOPIC = BASE_DEVICE_API_TOPIC + ATTRIBUTES;
    public static final String DEVICE_PROVISION_REQUEST_TOPIC = PROVISION + REQUEST;
    public static final String DEVICE_PROVISION_RESPONSE_TOPIC = PROVISION + RESPONSE;
    // v1 gateway topics
    public static final String BASE_GATEWAY_API_TOPIC = "v1/gateway";
    public static final String GATEWAY_CONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC = BASE_GATEWAY_API_TOPIC + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC = BASE_GATEWAY_API_TOPIC + CLAIM;
    public static final String GATEWAY_RPC_TOPIC = BASE_GATEWAY_API_TOPIC + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_RESPONSE;
    // v2 topics
    public static final String BASE_DEVICE_API_TOPIC_V2 = "v2";
    public static final String REQUEST_ID_PATTERN = "(?<requestId>\\d+)";
    public static final String CHUNK_PATTERN = "(?<chunk>\\d+)";
    public static final String DEVICE_FIRMWARE_REQUEST_TOPIC_PATTERN = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + REQUEST + "/" + REQUEST_ID_PATTERN + CHUNK + CHUNK_PATTERN;
    public static final String DEVICE_FIRMWARE_RESPONSES_TOPIC = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + RESPONSE + "/" + SUB_TOPIC + CHUNK + SUB_TOPIC;
    public static final String DEVICE_FIRMWARE_ERROR_TOPIC = BASE_DEVICE_API_TOPIC_V2 + FIRMWARE + ERROR;
    public static final String DEVICE_SOFTWARE_FIRMWARE_RESPONSES_TOPIC_FORMAT = BASE_DEVICE_API_TOPIC_V2 + "/%s" + RESPONSE + "/%s" + CHUNK + "%d";
    public static final String DEVICE_SOFTWARE_REQUEST_TOPIC_PATTERN = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + REQUEST + "/" + REQUEST_ID_PATTERN + CHUNK + CHUNK_PATTERN;
    public static final String DEVICE_SOFTWARE_RESPONSES_TOPIC = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + RESPONSE + "/" + SUB_TOPIC + CHUNK + SUB_TOPIC;
    public static final String DEVICE_SOFTWARE_ERROR_TOPIC = BASE_DEVICE_API_TOPIC_V2 + SOFTWARE + ERROR;
    public static final String DEVICE_ATTRIBUTES_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT;
    public static final String DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT + "/" + JSON_SHORT;
    public static final String DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_SHORT + "/" + PROTO_SHORT;
    public static final String DEVICE_TELEMETRY_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT;
    public static final String DEVICE_TELEMETRY_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT + "/" + JSON_SHORT;
    public static final String DEVICE_TELEMETRY_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + TELEMETRY_SHORT + "/" + PROTO_SHORT;
    public static final String DEVICE_RPC_RESPONSE_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT;
    public static final String DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT + JSON_SHORT + "/";
    public static final String DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_RESPONSE_SHORT + PROTO_SHORT + "/";
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC + SUB_TOPIC;
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC  + JSON_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC = DEVICE_RPC_RESPONSE_SHORT_TOPIC  + PROTO_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_SHORT_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT;
    public static final String DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT + JSON_SHORT + "/";
    public static final String DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC = BASE_DEVICE_API_TOPIC_V2 + DEVICE_RPC_REQUEST_SHORT + PROTO_SHORT + "/";
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + JSON_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC = DEVICE_RPC_REQUESTS_SHORT_TOPIC + PROTO_SHORT + "/" + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_RESPONSE_SHORT;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + JSON_SHORT + "/";
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX = DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX + PROTO_SHORT + "/";
    public static final String DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC_V2 + ATTRIBUTES_REQUEST_SHORT;
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX = DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX + JSON_SHORT + "/";
    public static final String DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX = DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX + PROTO_SHORT + "/";

    private MqttTopics() {
    }
}
