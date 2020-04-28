/*
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

'use strict';

const config = require('config'),
    JsInvokeMessageProcessor = require('../api/jsInvokeMessageProcessor'),
    logger = require('../config/logger')._logger('pubSubTemplate');
const {PubSub} = require('@google-cloud/pubsub');

const projectId = config.get('pubsub.project_id');
const credentials = JSON.parse(config.get('pubsub.service_account'));
const requestTopic = config.get('request_topic');

let pubSubClient;

function PubSubProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        let data = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });
        let dataBuffer = Buffer.from(data);
        return pubSubClient.topic(responseTopic).publish(dataBuffer);
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
        pubSubClient = new PubSub({projectId: projectId, credentials: credentials});

        const subscription = pubSubClient.subscription(requestTopic);

        const messageProcessor = new JsInvokeMessageProcessor(new PubSubProducer());

        const messageHandler = message => {

            messageProcessor.onJsInvokeMessage(message.data.toString('utf8'));
            message.ack();
        };

        subscription.on('message', messageHandler);

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (pubSubClient) {
        logger.info('Stopping Pub/Sub client.')
        try {
            await pubSubClient.close();
            logger.info('Pub/Sub client is stopped.')
            process.exit(status);
        } catch (e) {
            logger.info('Pub/Sub client stop error.');
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}

