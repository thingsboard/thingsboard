///
/// Copyright © 2016-2022 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import config from 'config';
import { _logger } from '../config/logger';
import { JsInvokeMessageProcessor } from '../api/jsInvokeMessageProcessor'
import { IQueue } from './queue.models';
import {
    CreateQueueCommand,
    CreateQueueRequest,
    DeleteMessageBatchCommand,
    DeleteMessageBatchRequest,
    DeleteMessageBatchRequestEntry,
    ListQueuesCommand,
    ListQueuesResult,
    ReceiveMessageCommand,
    ReceiveMessageRequest,
    ReceiveMessageResult,
    SendMessageCommand,
    SendMessageRequest,
    SQSClient
} from '@aws-sdk/client-sqs';
import uuid from 'uuid-random';

export class AwsSqsTemplate implements IQueue {

    private logger = _logger(`awsSqsTemplate`);
    private requestTopic: string = config.get('request_topic');
    private accessKeyId: string = config.get('aws_sqs.access_key_id');
    private secretAccessKey: string = config.get('aws_sqs.secret_access_key');
    private region: string = config.get('aws_sqs.region');
    private queueProperties: string = config.get('aws_sqs.queue_properties');
    private pollInterval = Number(config.get('js.response_poll_interval'));

    private sqsClient: SQSClient;
    private requestQueueURL: string
    private queueUrls = new Map<string, string>();
    private queueAttributes: { [n: string]: string } = {
        FifoQueue: 'true'
    };
    private timer: NodeJS.Timer;

    name = 'AWS SQS';

    constructor() {
    }

    async init() {
        this.sqsClient = new SQSClient({
            apiVersion: '2012-11-05',
            credentials: {
                accessKeyId: this.accessKeyId,
                secretAccessKey: this.secretAccessKey
            },
            region: this.region
        });

        const queues = await this.getQueues();

        if (queues.QueueUrls) {
            queues.QueueUrls.forEach(queueUrl => {
                const delimiterPosition = queueUrl.lastIndexOf('/');
                const queueName = queueUrl.substring(delimiterPosition + 1);
                this.queueUrls.set(queueName, queueUrl);
            });
        }

        this.parseQueueProperties();

        this.requestQueueURL = this.queueUrls.get(AwsSqsTemplate.topicToSqsQueueName(this.requestTopic)) || '';
        if (!this.requestQueueURL) {
            this.requestQueueURL = await this.createQueue(this.requestTopic);
        }

        const messageProcessor = new JsInvokeMessageProcessor(this);

        const params: ReceiveMessageRequest = {
            MaxNumberOfMessages: 10,
            QueueUrl: this.requestQueueURL,
            WaitTimeSeconds: Math.ceil(this.pollInterval / 10)
        };
        this.timer = setTimeout(() => {this.getAndProcessMessage(messageProcessor, params)}, this.pollInterval);
    }

    private async getAndProcessMessage(messageProcessor: JsInvokeMessageProcessor, params: ReceiveMessageRequest) {
        const messagesResponse: ReceiveMessageResult = await this.sqsClient.send(new ReceiveMessageCommand(params));
        const messages = messagesResponse.Messages;

        if (messages && messages.length > 0) {
            const entries: DeleteMessageBatchRequestEntry[] = [];

            messages.forEach(message => {
                entries.push({
                    Id: message.MessageId,
                    ReceiptHandle: message.ReceiptHandle
                });
                messageProcessor.onJsInvokeMessage(JSON.parse(message.Body || ''));
            });

            const deleteBatch: DeleteMessageBatchRequest = {
                QueueUrl: this.requestQueueURL,
                Entries: entries
            };
            try {
                await this.sqsClient.send(new DeleteMessageBatchCommand(deleteBatch))
            } catch (err: any) {
                this.logger.error("Failed to delete messages from queue.", err.message);
            }
        }
        this.timer = setTimeout(() => {this.getAndProcessMessage(messageProcessor, params)}, this.pollInterval);
    }

    async send(responseTopic: string, scriptId: string, rawResponse: Buffer, headers: any): Promise<any> {
        let msgBody = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });

        let responseQueueUrl = this.queueUrls.get(AwsSqsTemplate.topicToSqsQueueName(responseTopic));

        if (!responseQueueUrl) {
            responseQueueUrl = await this.createQueue(responseTopic);
            this.queueUrls.set(responseTopic, responseQueueUrl);
        }

        let msgId = uuid();

        let params: SendMessageRequest = {
            MessageBody: msgBody,
            QueueUrl: responseQueueUrl,
            MessageGroupId: msgId,
            MessageDeduplicationId: msgId
        };

        return this.sqsClient.send(new SendMessageCommand(params))
    }

    private async getQueues(): Promise<ListQueuesResult> {
        return this.sqsClient.send(new ListQueuesCommand({}));
    }

    private parseQueueProperties() {
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            this.queueAttributes[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
        });
    }

    private static topicToSqsQueueName(topic: string): string {
        return topic.replace(/\./g, '_') + '.fifo';
    }

    private async createQueue(topic: string): Promise<string> {
        let queueName = AwsSqsTemplate.topicToSqsQueueName(topic);
        let queueParams: CreateQueueRequest = {
            QueueName: queueName,
            Attributes: this.queueAttributes
        };

        const result = await this.sqsClient.send(new CreateQueueCommand(queueParams));
        return result.QueueUrl || '';
    }

    async destroy(): Promise<void> {
        this.logger.info('Stopping AWS SQS resources...');
        clearTimeout(this.timer);
        if (this.sqsClient) {
            this.logger.info('Stopping AWS SQS client...');
            try {
                const _sqsClient = this.sqsClient;
                // @ts-ignore
                delete this.sqsClient;
                _sqsClient.destroy();
                this.logger.info('AWS SQS client stopped.');
            } catch (e: any) {
                this.logger.info('AWS SQS client stop error.');
            }
        }
        this.logger.info('AWS SQS resources stopped.')
    }
}
