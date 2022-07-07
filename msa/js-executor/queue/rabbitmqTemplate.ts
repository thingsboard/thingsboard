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
import amqp, { ConfirmChannel, Connection } from 'amqplib';
import { Options, Replies } from 'amqplib/properties';
import { sleep } from '../api/utils';

export class RabbitMqTemplate implements IQueue {

    private logger = _logger(`rabbitmqTemplate`);
    private requestTopic: string = config.get('request_topic');
    private host = config.get('rabbitmq.host');
    private port = config.get('rabbitmq.port');
    private vhost = config.get('rabbitmq.virtual_host');
    private username = config.get('rabbitmq.username');
    private password = config.get('rabbitmq.password');
    private queueProperties: string = config.get('rabbitmq.queue_properties');
    private pollInterval = Number(config.get('js.response_poll_interval'));

    private queueOptions: Options.AssertQueue = {
        durable: false,
        exclusive: false,
        autoDelete: false
    };
    private connection: Connection;
    private channel: ConfirmChannel;
    private stopped = false;
    private topics: string[] = [];

    constructor() {
    }

    async init(): Promise<void> {
        try {
            const url = `amqp://${this.username}:${this.password}@${this.host}:${this.port}${this.vhost}`;
            this.connection = await amqp.connect(url);
            this.channel = await this.connection.createConfirmChannel();

            this.parseQueueProperties();

            await this.createQueue(this.requestTopic);

            const messageProcessor = new JsInvokeMessageProcessor(this);

            while (!this.stopped) {
                let pollStartTs = new Date().getTime();
                let message = await this.channel.get(this.requestTopic);

                if (message) {
                    messageProcessor.onJsInvokeMessage(JSON.parse(message.content.toString('utf8')));
                    this.channel.ack(message);
                } else {
                    let pollDuration = new Date().getTime() - pollStartTs;
                    if (pollDuration < this.pollInterval) {
                        await sleep(this.pollInterval - pollDuration);
                    }
                }
            }
        } catch (e: any) {
            this.logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
            this.logger.error(e.stack);
            await this.destroy(-1);
        }
    }

    async send(responseTopic: string, scriptId: string, rawResponse: Buffer, headers: any): Promise<any> {

        if (!this.topics.includes(responseTopic)) {
            await this.createQueue(responseTopic);
            this.topics.push(responseTopic);
        }

        let data = JSON.stringify(
            {
                key: scriptId,
                data: [...rawResponse],
                headers: headers
            });
        let dataBuffer = Buffer.from(data);
        this.channel.sendToQueue(responseTopic, dataBuffer);
        return this.channel.waitForConfirms()
    }

    private parseQueueProperties() {
        let args: { [n: string]: number } = {};
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            args[p.substring(0, delimiterPosition)] = Number(p.substring(delimiterPosition + 1));
        });
        this.queueOptions['arguments'] = args;
    }

    private async createQueue(topic: string): Promise<Replies.AssertQueue> {
        return this.channel.assertQueue(topic, this.queueOptions);
    }

    static async build(): Promise<RabbitMqTemplate> {
        const queue = new RabbitMqTemplate();
        await queue.init();
        return queue;
    }

    async destroy(status: number) {
        this.logger.info('Exiting with status: %d ...', status);
        this.logger.info('Stopping RabbitMQ resources...');

        if (this.channel) {
            this.logger.info('Stopping RabbitMQ chanel...');
            const _channel = this.channel;
            // @ts-ignore
            delete this.channel;
            await _channel.close();
            this.logger.info('RabbitMQ chanel stopped');
        }

        if (this.connection) {
            this.logger.info('Stopping RabbitMQ connection...')
            try {
                const _connection = this.connection;
                // @ts-ignore
                delete this.connection;
                await _connection.close();
                this.logger.info('RabbitMQ client connection.');
            } catch (e) {
                this.logger.info('RabbitMQ connection stop error.');
            }
        }
        this.logger.info('RabbitMQ resources stopped.')
        process.exit(status);
    }

}
