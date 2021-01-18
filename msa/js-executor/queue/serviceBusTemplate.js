/*
 * Copyright © 2016-2021 The Thingsboard Authors
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

const config = require('config'),
    JsInvokeMessageProcessor = require('../api/jsInvokeMessageProcessor'),
    logger = require('../config/logger')._logger('serviceBusTemplate');
const {ServiceBusClient, ReceiveMode} = require("@azure/service-bus");
const azure = require('azure-sb');

const requestTopic = config.get('request_topic');
const namespaceName = config.get('service_bus.namespace_name');
const sasKeyName = config.get('service_bus.sas_key_name');
const sasKey = config.get('service_bus.sas_key');
const queueProperties = config.get('service_bus.queue_properties');

let sbClient;
let receiverClient;
let receiver;
let serviceBusService;

let queueOptions = {};
const queues = [];
const senderMap = new Map();

function ServiceBusProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        if (!queues.includes(requestTopic)) {
            await createQueueIfNotExist(requestTopic);
            queues.push(requestTopic);
        }

        let customSender = senderMap.get(responseTopic);

        if (!customSender) {
            customSender = new CustomSender(responseTopic);
            senderMap.set(responseTopic, customSender);
        }

        let data = {
            key: scriptId,
            data: [...rawResponse],
            headers: headers
        };

        return customSender.send({body: data});
    }
}

function CustomSender(topic) {
    this.queueClient = sbClient.createQueueClient(topic);
    this.sender = this.queueClient.createSender();

    this.send = async (message) => {
        return this.sender.send(message);
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const connectionString = `Endpoint=sb://${namespaceName}.servicebus.windows.net/;SharedAccessKeyName=${sasKeyName};SharedAccessKey=${sasKey}`;
        sbClient = ServiceBusClient.createFromConnectionString(connectionString);
        serviceBusService = azure.createServiceBusService(connectionString);

        parseQueueProperties();

        await new Promise((resolve, reject) => {
            serviceBusService.listQueues((err, data) => {
                if (err) {
                    reject(err);
                } else {
                    data.forEach(queue => {
                        queues.push(queue.QueueName);
                    });
                    resolve();
                }
            });
        });

        if (!queues.includes(requestTopic)) {
            await createQueueIfNotExist(requestTopic);
            queues.push(requestTopic);
        }

        receiverClient = sbClient.createQueueClient(requestTopic);
        receiver = receiverClient.createReceiver(ReceiveMode.peekLock);

        const messageProcessor = new JsInvokeMessageProcessor(new ServiceBusProducer());

        const messageHandler = async (message) => {
            if (message) {
                messageProcessor.onJsInvokeMessage(message.body);
                await message.complete();
            }
        };
        const errorHandler = (error) => {
            logger.error('Failed to receive message from queue.', error);
        };
        receiver.registerMessageHandler(messageHandler, errorHandler);
    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

async function createQueueIfNotExist(topic) {
    return new Promise((resolve, reject) => {
        serviceBusService.createQueueIfNotExists(topic, queueOptions, (err) => {
            if (err) {
                reject(err);
            } else {
                resolve();
            }
        });
    });
}

function parseQueueProperties() {
    let properties = {};
    const props = queueProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        properties[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
    });
    queueOptions = {
        DuplicateDetection: 'false',
        MaxSizeInMegabytes: properties['maxSizeInMb'],
        DefaultMessageTimeToLive: `PT${properties['messageTimeToLiveInSec']}S`,
        LockDuration: `PT${properties['lockDurationInSec']}S`
    };
}

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    logger.info('Stopping Azure Service Bus resources...')
    if (receiver) {
        try {
            await receiver.close();
        } catch (e) {

        }
    }

    if (receiverClient) {
        try {
            await receiverClient.close();
        } catch (e) {

        }
    }

    senderMap.forEach((k, v) => {
        try {
            v.sender.close();
        } catch (e) {

        }
        try {
            v.queueClient.close();
        } catch (e) {

        }
    });

    if (sbClient) {
        try {
            sbClient.close();
        } catch (e) {

        }
    }
    logger.info('Azure Service Bus resources stopped.')
    process.exit(status);
}