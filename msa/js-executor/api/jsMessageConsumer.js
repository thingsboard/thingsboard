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

var logger = require('../config/logger')('JsMessageConsumer');

var js = require('./jsinvoke').js;


exports.onJsInvokeMessage = function(message, producer) {

    logger.info('Received message: %s', JSON.stringify(message));

    var request = js.RemoteJsRequest.decode(message.value);

    logger.info('Received request: %s', JSON.stringify(request));

    if (request.compileRequest) {
        var compileResponse = js.JsCompileResponse.create(
            {
                errorCode: js.JsInvokeErrorCode.COMPILATION_ERROR,
                success: false,
                errorDetails: 'Not Implemented!',
                scriptIdLSB: request.compileRequest.scriptIdLSB,
                scriptIdMSB: request.compileRequest.scriptIdMSB
            }
        );
        var response = js.RemoteJsResponse.create(
            {
                compileResponse: compileResponse
            }
        );
        var rawResponse = js.RemoteJsResponse.encode(response).finish();
        sendMessage(producer, rawResponse);
    }
}

function sendMessage(producer, rawMessage) {

}
