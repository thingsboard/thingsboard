/*
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

'use strict';

const logger = require('../config/logger')('JsMessageConsumer');
const Utils = require('./Utils');
const js = require('./jsinvoke.proto').js;
const KeyedMessage = require('kafka-node').KeyedMessage;


exports.onJsInvokeMessage = function(message, producer) {

    logger.info('Received message: %s', JSON.stringify(message));

    var request = js.RemoteJsRequest.decode(message.value);

    logger.info('Received request: %s', JSON.stringify(request));

    var requestId = getRequestId(request);

    logger.info('Received request, responseTopic: [%s]; requestId: [%s]', request.responseTopic, requestId);

    if (request.compileRequest) {
        var scriptId = getScriptId(request.compileRequest);

        logger.info('Received compile request, scriptId: [%s]', scriptId);

        var compileResponse = js.JsCompileResponse.create(
            {
                errorCode: js.JsInvokeErrorCode.COMPILATION_ERROR,
                success: false,
                errorDetails: 'Not Implemented!',
                scriptIdLSB: request.compileRequest.scriptIdLSB,
                scriptIdMSB: request.compileRequest.scriptIdMSB
            }
        );
        const requestIdBits = Utils.UUIDToBits(requestId);
        var response = js.RemoteJsResponse.create(
            {
                requestIdMSB: requestIdBits[0],
                requestIdLSB: requestIdBits[1],
                compileResponse: compileResponse
            }
        );
        var rawResponse = js.RemoteJsResponse.encode(response).finish();
        sendMessage(producer, rawResponse, request.responseTopic, scriptId);
    }
}

function sendMessage(producer, rawMessage, responseTopic, scriptId) {
    const message = new KeyedMessage(scriptId, rawMessage);
    const payloads = [ { topic: responseTopic, messages: rawMessage, key: scriptId } ];
    producer.send(payloads, function (err, data) {
        console.log(data);
    });
}

function getScriptId(request) {
    return Utils.toUUIDString(request.scriptIdMSB, request.scriptIdLSB);
}

function getRequestId(request) {
    return Utils.toUUIDString(request.requestIdMSB, request.requestIdLSB);
}
