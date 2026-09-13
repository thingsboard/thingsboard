// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

public enum CoapSessionMsgType {

    GET_ATTRIBUTES_REQUEST,
    POST_ATTRIBUTES_REQUEST,
    SUBSCRIBE_ATTRIBUTES_REQUEST,
    UNSUBSCRIBE_ATTRIBUTES_REQUEST,
    POST_TELEMETRY_REQUEST,
    SUBSCRIBE_RPC_COMMANDS_REQUEST,
    UNSUBSCRIBE_RPC_COMMANDS_REQUEST,
    TO_DEVICE_RPC_RESPONSE,
    TO_SERVER_RPC_REQUEST,
    CLAIM_REQUEST;

}
