/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt;

/**
 * Created by ashvayka on 19.01.17.
 */
public class MqttTopics {

    private static final String RPC = "/rpc";
    private static final String CONNECT = "/connect";
    private static final String DISCONNECT = "/disconnect";
    private static final String TELEMETRY = "/telemetry";
    private static final String ATTRIBUTES = "/attributes";
    private static final String CLAIM = "/claim";
    private static final String SUB_TOPIC = "+";
    private static final String ATTRIBUTES_RESPONSE = "/attributes/response";
    private static final String ATTRIBUTES_REQUEST = "/attributes/request";

    private static final String DEVICE_RPC_RESPONSE = "/rpc/response/";
    private static final String DEVICE_RPC_REQUEST = "/rpc/request/";

    private static final String DEVICE_ATTRIBUTES_RESPONSE = ATTRIBUTES_RESPONSE + "/";
    private static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";

    // V1_JSON topics

    public static final String BASE_DEVICE_API_TOPIC_V1_JSON = "v1/devices/me";

    public static final String DEVICE_RPC_RESPONSE_TOPIC_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + DEVICE_RPC_RESPONSE;
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC_V1_JSON = DEVICE_RPC_RESPONSE_TOPIC_V1_JSON + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + DEVICE_RPC_REQUEST;
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC_V1_JSON = DEVICE_RPC_REQUESTS_TOPIC_V1_JSON + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + DEVICE_ATTRIBUTES_RESPONSE;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V1_JSON = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V1_JSON + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + DEVICE_ATTRIBUTES_REQUEST;
    public static final String DEVICE_TELEMETRY_TOPIC_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + CLAIM;
    public static final String DEVICE_ATTRIBUTES_TOPIC_V1_JSON = BASE_DEVICE_API_TOPIC_V1_JSON + ATTRIBUTES;

    // V1_JSON gateway topics

    public static final String BASE_GATEWAY_API_TOPIC_V1_JSON = "v1/gateway";
    public static final String GATEWAY_CONNECT_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + CLAIM;
    public static final String GATEWAY_RPC_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V1_JSON = BASE_GATEWAY_API_TOPIC_V1_JSON + ATTRIBUTES_RESPONSE;

    // V2_JSON topics

    public static final String BASE_DEVICE_API_TOPIC_V2_JSON = "v2/json";

    public static final String DEVICE_RPC_RESPONSE_TOPIC_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + DEVICE_RPC_RESPONSE;
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC_V2_JSON = DEVICE_RPC_RESPONSE_TOPIC_V2_JSON + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + DEVICE_RPC_REQUEST;
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_JSON = DEVICE_RPC_REQUESTS_TOPIC_V2_JSON + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + DEVICE_ATTRIBUTES_RESPONSE;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V2_JSON = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V2_JSON + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + DEVICE_ATTRIBUTES_REQUEST;
    public static final String DEVICE_TELEMETRY_TOPIC_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + CLAIM;
    public static final String DEVICE_ATTRIBUTES_TOPIC_V2_JSON = BASE_DEVICE_API_TOPIC_V2_JSON + ATTRIBUTES;

    // V2_PROTO topics

    public static final String BASE_DEVICE_API_TOPIC_V2_PROTO = "v2/proto";

    public static final String DEVICE_RPC_RESPONSE_TOPIC_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + DEVICE_RPC_RESPONSE;
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC_V2_PROTO = DEVICE_RPC_RESPONSE_TOPIC_V2_PROTO + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + DEVICE_RPC_REQUEST;
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC_V2_PROTO = DEVICE_RPC_REQUESTS_TOPIC_V2_PROTO + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + DEVICE_ATTRIBUTES_RESPONSE;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC_V2_PROTO = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX_V2_PROTO + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + DEVICE_ATTRIBUTES_REQUEST;
    public static final String DEVICE_TELEMETRY_TOPIC_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + CLAIM;
    public static final String DEVICE_ATTRIBUTES_TOPIC_V2_PROTO = BASE_DEVICE_API_TOPIC_V2_PROTO + ATTRIBUTES;


    public static final String BASE_GATEWAY_API_V2_TOPIC = "v2/gateway";

    // V2_JSON gateway topics

    public static final String BASE_GATEWAY_API_TOPIC_V2_JSON = BASE_GATEWAY_API_V2_TOPIC + "/json";

    public static final String GATEWAY_CONNECT_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + CLAIM;
    public static final String GATEWAY_RPC_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V2_JSON = BASE_GATEWAY_API_TOPIC_V2_JSON + ATTRIBUTES_RESPONSE;

    // V2_PROTO gateway topics

    public static final String BASE_GATEWAY_API_TOPIC_V2_PROTO = BASE_GATEWAY_API_V2_TOPIC + "/proto";

    public static final String GATEWAY_CONNECT_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + CLAIM;
    public static final String GATEWAY_RPC_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC_V2_PROTO = BASE_GATEWAY_API_TOPIC_V2_PROTO + ATTRIBUTES_RESPONSE;

    private MqttTopics() {
    }
}
