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
    logger = require('../config/logger')._logger('awsSqsTemplate');

const requestTopic = config.get('request_topic');

const accessKeyId = config.get('aws_sqs.access_key_id');
const secretAccessKey = config.get('aws_sqs.secret_access_key');
const region = config.get('aws_sqs.region');
const AWS = require('aws-sdk');

let sqsClient;
let queueURL;
let responseTopics = new Map();
let stopped = false;

function AwsSqsProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {
        let msgBody = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });

        let responseQueueUrl = responseTopics.get(responseTopic);

        if (!responseQueueUrl) {
            responseQueueUrl = await createQueue(responseTopic);
            responseTopics.set(responseTopic, responseQueueUrl);
        }

        let params = {MessageBody: msgBody, QueueUrl: responseQueueUrl, MessageGroupId: scriptId};

        return new Promise((resolve, reject) => {
            sqsClient.sendMessage(params, function (err, data) {
                if (err) {
                    reject(err);
                } else {
                    resolve(data);
                }
            });
        });
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
        AWS.config.update({accessKeyId: accessKeyId, secretAccessKey: secretAccessKey, region: region});

        sqsClient = new AWS.SQS({apiVersion: '2012-11-05'});

        queueURL = await createQueue(requestTopic);
        const messageProcessor = new JsInvokeMessageProcessor(new AwsSqsProducer());

        const params = {
            MaxNumberOfMessages: 10,
            QueueUrl: queueURL,
            WaitTimeSeconds: 0.025
        };
        while (!stopped) {
            const messages = await new Promise((resolve, reject) => {
                sqsClient.receiveMessage(params, function (err, data) {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(data.Messages);
                    }
                });
            });

            if (messages && messages.length > 0) {
                const entries = [];

                messages.forEach(message => {
                    entries.push({
                        Id: message.MessageId,
                        ReceiptHandle: message.ReceiptHandle
                    });
                    messageProcessor.onJsInvokeMessage(message.Body);
                });

                const deleteBatch = {
                    QueueUrl: queueURL,
                    Entries: entries
                };
                sqsClient.deleteMessageBatch(deleteBatch, function (err, data) {
                    if (err) {
                        logger.error("Failed to delete messages from queue.", err.message);
                    } else {
                        //do nothing
                    }
                });
            }
        }
    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

function createQueue(topic) {
    let queueName = topic.replace(/\./g, '_') + '.fifo';
    let queueParams = {
        QueueName: queueName, Attributes: {
            FifoQueue: 'true',
            ContentBasedDeduplication: 'true'

        }
    };
    return new Promise((resolve, reject) => {
        sqsClient.createQueue(queueParams, function (err, data) {
            if (err) {
                reject(err);
            } else {
                resolve(data.QueueUrl);
            }
        });
    });
}

process.on('exit', () => {
    stopped = true;
    logger.info('Aws Sqs client stopped.');
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (sqsClient) {
        logger.info('Stopping Aws Sqs client.')
        try {
            await sqsClient.close();
            logger.info('Aws Sqs client is stopped.')
            process.exit(status);
        } catch (e) {
            logger.info('Aws Sqs client stop error.');
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}
