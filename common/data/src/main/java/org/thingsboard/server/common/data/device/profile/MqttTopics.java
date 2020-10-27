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
    private static final String ACTION = "/action";

    private static final String ATTRIBUTES_RESPONSE = ATTRIBUTES + RESPONSE;
    private static final String ATTRIBUTES_REQUEST = ATTRIBUTES + REQUEST;

    private static final String DEVICE_RPC_RESPONSE = RPC + RESPONSE + "/";
    private static final String DEVICE_RPC_REQUEST = RPC + REQUEST + "/";

    private static final String DEVICE_ATTRIBUTES_RESPONSE = ATTRIBUTES_RESPONSE + "/";
    private static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";

    public static final String PAYLOAD_TYPE_TOPIC = "/payload";
    // V1_JSON topics

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

    // V1_JSON gateway topics

    public static final String BASE_GATEWAY_API_TOPIC = "v1/gateway";
    public static final String GATEWAY_CONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC = BASE_GATEWAY_API_TOPIC + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC = BASE_GATEWAY_API_TOPIC + CLAIM;
    public static final String GATEWAY_RPC_TOPIC = BASE_GATEWAY_API_TOPIC + RPC;
    public static final String GATEWAY_DEVICE_ACTION_TOPIC = BASE_GATEWAY_API_TOPIC + ACTION;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_RESPONSE;

    //V2_PROTO gateway topics

    private static final String MINIMIZED_REQUEST = "/req";
    private static final String MINIMIZED_RESPONSE = "/rsp";
    private static final String MINIMIZED_RPC = "/rpc";
    private static final String MINIMIZED_CONNECT = "/con";
    private static final String MINIMIZED_DISCONNECT = "/dis";
    private static final String MINIMIZED_TELEMETRY = "/tel";
    private static final String MINIMIZED_ATTRIBUTES = "/atr";
    private static final String MINIMIZED_CLAIM = "/clm";
    private static final String MINIMIZED_ACTION = "/act";

    private static final String MINIMIZED_ATTRIBUTES_RESPONSE = MINIMIZED_ATTRIBUTES + MINIMIZED_RESPONSE;
    private static final String MINIMIZED_ATTRIBUTES_REQUEST = MINIMIZED_ATTRIBUTES + MINIMIZED_REQUEST;

    public static final String MINIMIZED_BASE_GATEWAY_API_TOPIC = "v2/g";
    public static final String MINIMIZED_GATEWAY_CONNECT_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_CONNECT;
    public static final String MINIMIZED_GATEWAY_DISCONNECT_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_DISCONNECT;
    public static final String MINIMIZED_GATEWAY_ATTRIBUTES_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_ATTRIBUTES;
    public static final String MINIMIZED_GATEWAY_TELEMETRY_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_TELEMETRY;
    public static final String MINIMIZED_GATEWAY_CLAIM_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_CLAIM;
    public static final String MINIMIZED_GATEWAY_RPC_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_RPC;
    public static final String MINIMIZED_GATEWAY_DEVICE_ACTION_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_ACTION;
    public static final String MINIMIZED_GATEWAY_ATTRIBUTES_REQUEST_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_ATTRIBUTES_REQUEST;
    public static final String MINIMIZED_GATEWAY_ATTRIBUTES_RESPONSE_TOPIC = MINIMIZED_BASE_GATEWAY_API_TOPIC + MINIMIZED_ATTRIBUTES_RESPONSE;

    private MqttTopics() {
    }
}
