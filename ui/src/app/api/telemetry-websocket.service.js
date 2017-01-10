/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
import 'angular-websocket';
import thingsboardTypes from '../common/types.constant';

export default angular.module('thingsboard.api.telemetryWebsocket', [thingsboardTypes])
    .factory('telemetryWebsocketService', TelemetryWebsocketService)
    .name;

/*@ngInject*/
function TelemetryWebsocketService($websocket, $timeout, $window, types, userService) {

    var isOpening = false,
        isOpened = false,
        lastCmdId = 0,
        subscribers = {},
        subscribersCount = 0,
        cmdsWrapper = {
            tsSubCmds: [],
            historyCmds: [],
            attrSubCmds: []
        },
        telemetryUri,
        dataStream,
        location = $window.location,
        socketCloseTimer;

    if (location.protocol === "https:") {
        telemetryUri = "wss:";
    } else {
        telemetryUri = "ws:";
    }
    telemetryUri += "//" + location.hostname + ":" + location.port;
    telemetryUri += "/api/ws/plugins/telemetry";

    var service = {
        subscribe: subscribe,
        unsubscribe: unsubscribe
    }

    return service;

    function publishCommands () {
        if (isOpened && (cmdsWrapper.tsSubCmds.length > 0 ||
            cmdsWrapper.historyCmds.length > 0 ||
            cmdsWrapper.attrSubCmds.length > 0)) {
            dataStream.send(angular.copy(cmdsWrapper)).then(function () {
                checkToClose();
            });
            cmdsWrapper.tsSubCmds = [];
            cmdsWrapper.historyCmds = [];
            cmdsWrapper.attrSubCmds = [];
        }
        tryOpenSocket();
    }

    function onError (/*message*/) {
        isOpening = false;
    }

    function onOpen () {
        isOpening = false;
        isOpened = true;
        publishCommands();
    }

    function onClose () {
        isOpening = false;
        isOpened = false;
    }

    function onMessage (message) {
        if (message.data) {
            var data = angular.fromJson(message.data);
            if (data.subscriptionId) {
                var subscriber = subscribers[data.subscriptionId];
                if (subscriber && data.data) {
                    subscriber.onData(data.data);
                }
            }
        }
        checkToClose();
    }

    function nextCmdId () {
        lastCmdId++;
        return lastCmdId;
    }

    function subscribe (subscriber) {
        var cmdId = nextCmdId();
        subscribers[cmdId] = subscriber;
        subscribersCount++;
        if (angular.isDefined(subscriber.subscriptionCommand)) {
            subscriber.subscriptionCommand.cmdId = cmdId;
            if (subscriber.type === types.dataKeyType.timeseries) {
                cmdsWrapper.tsSubCmds.push(subscriber.subscriptionCommand);
            } else if (subscriber.type === types.dataKeyType.attribute) {
                cmdsWrapper.attrSubCmds.push(subscriber.subscriptionCommand);
            }
        } else if (angular.isDefined(subscriber.historyCommand)) {
            subscriber.historyCommand.cmdId = cmdId;
            cmdsWrapper.historyCmds.push(subscriber.historyCommand);
        }
        publishCommands();
    }

    function unsubscribe (subscriber) {
        if (subscriber.subscriptionCommand) {
            subscriber.subscriptionCommand.unsubscribe = true;
            if (subscriber.type === types.dataKeyType.timeseries) {
                cmdsWrapper.tsSubCmds.push(subscriber.subscriptionCommand);
            } else if (subscriber.type === types.dataKeyType.attribute) {
                cmdsWrapper.attrSubCmds.push(subscriber.subscriptionCommand);
            }
            delete subscribers[subscriber.subscriptionCommand.cmdId];
        } else if (subscriber.historyCommand) {
            delete subscribers[subscriber.historyCommand.cmdId];
        }
        subscribersCount--;
        publishCommands();
    }

    function checkToClose () {
        if (subscribersCount === 0 && isOpened) {
            if (!socketCloseTimer) {
                socketCloseTimer = $timeout(closeSocket, 90000, false);
            }
        }
    }

    function tryOpenSocket () {
        if (!isOpened && !isOpening) {
            isOpening = true;
            dataStream = $websocket(telemetryUri + '?token=' + userService.getJwtToken());
            dataStream.onError(onError);
            dataStream.onOpen(onOpen);
            dataStream.onClose(onClose);
            dataStream.onMessage(onMessage);
        }
        if (socketCloseTimer) {
            $timeout.cancel(socketCloseTimer);
        }
    }

    function closeSocket() {
        if (isOpened) {
            dataStream.close();
        }
    }
}
