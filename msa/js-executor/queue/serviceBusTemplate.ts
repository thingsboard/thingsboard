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
    CreateQueueOptions,
    ProcessErrorArgs,
    ServiceBusAdministrationClient,
    ServiceBusClient,
    ServiceBusReceivedMessage,
    ServiceBusReceiver,
    ServiceBusSender
} from '@azure/service-bus';

export class ServiceBusTemplate implements IQueue {

    private logger = _logger(`serviceBusTemplate`);
    private requestTopic: string = config.get('request_topic');
    private namespaceName = config.get('service_bus.namespace_name');
    private sasKeyName = config.get('service_bus.sas_key_name');
    private sasKey = config.get('service_bus.sas_key');
    private queueProperties: string = config.get('service_bus.queue_properties');

    private sbClient: ServiceBusClient;
    private serviceBusService: ServiceBusAdministrationClient;
    private queueOptions: CreateQueueOptions = {};
    private queues: string[] = [];
    private receiver: ServiceBusReceiver;
    private senderMap = new Map<string, ServiceBusSender>();

    constructor() {
    }

    async init() {
        try {
            this.logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

            const connectionString = `Endpoint=sb://${this.namespaceName}.servicebus.windows.net/;SharedAccessKeyName=${this.sasKeyName};SharedAccessKey=${this.sasKey}`;
            this.sbClient = new ServiceBusClient(connectionString)
            this.serviceBusService = new ServiceBusAdministrationClient(connectionString);

            this.parseQueueProperties();

            const listQueues = await this.serviceBusService.listQueues();
            for await (const queue of listQueues) {
                this.queues.push(queue.name);
            }

            if (!this.queues.includes(this.requestTopic)) {
                await this.createQueueIfNotExist(this.requestTopic);
                this.queues.push(this.requestTopic);
            }

            this.receiver = this.sbClient.createReceiver(this.requestTopic, {receiveMode: 'peekLock'});

            const messageProcessor = new JsInvokeMessageProcessor(this);

            const messageHandler = async (message: ServiceBusReceivedMessage) => {
                if (message) {
                    messageProcessor.onJsInvokeMessage(message.body);
                    await this.receiver.completeMessage(message);
                }
            };
            const errorHandler = async (error: ProcessErrorArgs) => {
                this.logger.error('Failed to receive message from queue.', error);
            };
            this.receiver.subscribe({processMessage: messageHandler, processError: errorHandler})
        } catch (e: any) {
            this.logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
            this.logger.error(e.stack);
            await this.exit(-1);
        }
    }

    async send(responseTopic: string, scriptId: string, rawResponse: Buffer, headers: any): Promise<any> {
        if (!this.queues.includes(this.requestTopic)) {
            await this.createQueueIfNotExist(this.requestTopic);
            this.queues.push(this.requestTopic);
        }

        let customSender = this.senderMap.get(responseTopic);

        if (!customSender) {
            customSender = this.sbClient.createSender(responseTopic);
            this.senderMap.set(responseTopic, customSender);
        }

        let data = {
            key: scriptId,
            data: [...rawResponse],
            headers: headers
        };

        return customSender.sendMessages({body: data});
    }

    private parseQueueProperties() {
        let properties: { [n: string]: string } = {};
        const props = this.queueProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            properties[p.substring(0, delimiterPosition)] = p.substring(delimiterPosition + 1);
        });
        this.queueOptions = {
            requiresDuplicateDetection: false,
            maxSizeInMegabytes: Number(properties['maxSizeInMb']),
            defaultMessageTimeToLive: `PT${properties['messageTimeToLiveInSec']}S`,
            lockDuration: `PT${properties['lockDurationInSec']}S`
        };
    }

    private async createQueueIfNotExist(topic: string) {
        try {
            await this.serviceBusService.createQueue(topic, this.queueOptions)
        } catch (err: any) {
            if (err && err.code !== "MessageEntityAlreadyExistsError") {
                throw new Error(err);
            }
        }
    }

    static async build(): Promise<ServiceBusTemplate> {
        const queue = new ServiceBusTemplate();
        await queue.init();
        return queue;
    }

    async exit(status: number) {
        this.logger.info('Exiting with status: %d ...', status);
        this.logger.info('Stopping Azure Service Bus resources...')
        if (this.receiver) {
            try {
                await this.receiver.close();
                // @ts-ignore
                delete this.receiver;
            } catch (e) {
            }
        }

        this.senderMap.forEach(k => {
            try {
                k.close();
            } catch (e) {
            }
        });
        this.senderMap.clear();

        if (this.sbClient) {
            try {
                await this.sbClient.close();
                // @ts-ignore
                delete this.sbClient;
            } catch (e) {
            }
        }
        this.logger.info('Azure Service Bus resources stopped.')
        process.exit(status);
    }
}
