///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import fs from 'node:fs';
import { _logger, KafkaJsWinstonLogCreator } from '../config/logger';
import { JsInvokeMessageProcessor } from '../api/jsInvokeMessageProcessor'
import { IQueue } from './queue.models';
import {
    Admin,
    CompressionTypes,
    Consumer,
    Kafka,
    KafkaConfig,
    logLevel,
    Partitioners,
    Producer,
    TopicMessages
} from 'kafkajs';
import { isNotEmptyStr } from '../api/utils';
import { KeyObject } from 'tls';

import process, { exit, kill } from 'process';

export class KafkaTemplate implements IQueue {

    private logger = _logger(`kafkaTemplate`);
    private replicationFactor = Number(config.get('kafka.replication_factor'));
    private topicProperties: string = config.get('kafka.topic_properties');
    private kafkaClientId: string = config.get('kafka.client_id');
    private acks = Number(config.get('kafka.acks'));
    private maxBatchSize = Number(config.get('kafka.batch_size'));
    private linger = Number(config.get('kafka.linger_ms'));
    private requestTimeout = Number(config.get('kafka.requestTimeout'));
    private connectionTimeout = Number(config.get('kafka.connectionTimeout'));
    private compressionType = (config.get('kafka.compression') === "gzip") ? CompressionTypes.GZIP : CompressionTypes.None;
    private partitionsConsumedConcurrently = Number(config.get('kafka.partitions_consumed_concurrently'));

    private kafkaClient: Kafka;
    private kafkaAdmin: Admin;
    private consumer: Consumer;
    private producer: Producer;
    private configEntries: any[] = [];
    private batchMessages: TopicMessages[] = [];
    private sendLoopInstance: NodeJS.Timeout;

    name = 'Kafka';

    constructor() {
    }

    async init(): Promise<void> {
        const kafkaBootstrapServers: string = config.get('kafka.bootstrap.servers');
        const queuePrefix: string = config.get('queue_prefix');
        const requestTopic: string = queuePrefix ? queuePrefix + "." + config.get('request_topic') : config.get('request_topic');
        const useConfluent = config.get('kafka.use_confluent_cloud');
        const enabledSsl = Boolean(config.get('kafka.ssl.enabled'));
        const groupId:string =  queuePrefix ? queuePrefix + ".js-executor-group" : "js-executor-group";
        this.logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        this.logger.info('Kafka Requests Topic: %s', requestTopic);

        let kafkaConfig: KafkaConfig = {
            brokers: kafkaBootstrapServers.split(','),
            logLevel: logLevel.INFO,
            logCreator: KafkaJsWinstonLogCreator
        };

        if (this.kafkaClientId) {
            kafkaConfig['clientId'] = this.kafkaClientId;
        } else {
            this.logger.warn('KAFKA_CLIENT_ID is undefined. Consider to define the env variable KAFKA_CLIENT_ID');
        }

        kafkaConfig['requestTimeout'] = this.requestTimeout;

        kafkaConfig['connectionTimeout'] = this.connectionTimeout;

        if (useConfluent) {
            kafkaConfig['sasl'] = {
                mechanism: config.get('kafka.confluent.sasl.mechanism') as any,
                username: config.get('kafka.confluent.username'),
                password: config.get('kafka.confluent.password')
            };
            kafkaConfig['ssl'] = true;
        }

        if (enabledSsl) {
            const certFilePath: string = config.has('kafka.ssl.cert_file') ? config.get('kafka.ssl.cert_file') : '';
            const keyFilePath: string = config.has('kafka.ssl.key_file') ? config.get('kafka.ssl.key_file') : '';
            const keyPassword: string = config.has('kafka.ssl.key_password') ? config.get('kafka.ssl.key_password') : '';
            const caFilePath: string = config.has('kafka.ssl.ca_file') ? config.get('kafka.ssl.ca_file') : '';

            kafkaConfig.ssl = {};

            if (isNotEmptyStr(certFilePath)) {
                kafkaConfig.ssl.cert = fs.readFileSync(certFilePath, 'utf-8');
            }

            if (isNotEmptyStr(keyFilePath)) {
                const keyConfig: KeyObject = {pem: fs.readFileSync(keyFilePath, 'utf-8')};
                if (isNotEmptyStr(keyPassword)) {
                    keyConfig.passphrase = keyPassword;
                }
                kafkaConfig.ssl.key = [keyConfig];
            }

            if (isNotEmptyStr(caFilePath)) {
                kafkaConfig.ssl.ca = fs.readFileSync(caFilePath, 'utf-8');
            }
        }

        this.parseTopicProperties();

        this.kafkaClient = new Kafka(kafkaConfig);
        this.kafkaAdmin = this.kafkaClient.admin();
        await this.kafkaAdmin.connect();

        let partitions = 1;

        for (let i = 0; i < this.configEntries.length; i++) {
            let param = this.configEntries[i];
            if (param.name === 'partitions') {
                partitions = param.value;
                this.configEntries.splice(i, 1);
                break;
            }
        }

        let topics = await this.kafkaAdmin.listTopics();

        if (!topics.includes(requestTopic)) {
            let createRequestTopicResult = await this.createTopic(requestTopic, partitions);
            if (createRequestTopicResult) {
                this.logger.info('Created new topic: %s', requestTopic);
            }
        }

        this.consumer = this.kafkaClient.consumer({groupId: groupId});
        this.producer = this.kafkaClient.producer({createPartitioner: Partitioners.DefaultPartitioner});

        const {CRASH} = this.consumer.events;

        this.consumer.on(CRASH, async (e) => {
            this.logger.error(`Got consumer CRASH event, should restart: ${e.payload.restart}`);
            if (!e.payload.restart) {
                this.logger.error('Going to exit due to not retryable error!');
                kill(process.pid, 'SIGTERM'); //sending signal to myself process to trigger the handler
                await this.destroy();
            }
        });

        const messageProcessor = new JsInvokeMessageProcessor(this);
        await this.consumer.connect();
        await this.producer.connect();
        this.sendLoopWithLinger();
        await this.consumer.subscribe({topic: requestTopic});

        await this.consumer.run({
            partitionsConsumedConcurrently: this.partitionsConsumedConcurrently,
            eachMessage: async ({topic, partition, message}) => {
                let headers = message.headers;
                let key = message.key || new Buffer([]);
                let msg = {
                    key: key.toString('utf8'),
                    data: message.value,
                    headers: {
                        data: headers
                    }
                };
                messageProcessor.onJsInvokeMessage(msg);
            },
        });
}

    async send(responseTopic: string, msgKey: string, rawResponse: Buffer, headers: any): Promise<any> {
        const message = {
            topic: responseTopic,
            messages: [{
                key: msgKey,
                value: rawResponse,
                headers: headers.data
            }]
        };

        await this.pushMessageToSendLater(message);
    }

    private async pushMessageToSendLater(message: TopicMessages) {
        this.batchMessages.push(message);
        if (this.batchMessages.length >= this.maxBatchSize) {
            await this.sendMessagesAsBatch(true);
        }
    }

    private async sendMessagesAsBatch(isImmediately = false): Promise<void> {
        if (this.sendLoopInstance) {
            clearTimeout(this.sendLoopInstance);
        }
        if (this.batchMessages.length > 0) {
            this.logger.debug('sendMessagesAsBatch, length: [%s], %s', this.batchMessages.length, isImmediately ? 'immediately' : '');
            const messagesToSend = this.batchMessages;
            this.batchMessages = [];
            try {
                await this.producer.sendBatch({
                    topicMessages: messagesToSend,
                    acks: this.acks,
                    compression: this.compressionType
                })
                this.logger.debug('Response batch sent to kafka, length: [%s]', messagesToSend.length);
            } catch (err: any) {
                this.logger.error('Failed batch send to kafka, length: [%s], pending to reprocess msgs', messagesToSend.length);
                this.logger.error(err.stack);
                this.batchMessages = messagesToSend.concat(this.batchMessages);
            }
        }
        this.sendLoopWithLinger();
    }

    private parseTopicProperties() {
        const props = this.topicProperties.split(';');
        props.forEach(p => {
            const delimiterPosition = p.indexOf(':');
            this.configEntries.push({
                name: p.substring(0, delimiterPosition),
                value: p.substring(delimiterPosition + 1)
            });
        });
    }

    private createTopic(topic: string, partitions: number): Promise<boolean> {
        return this.kafkaAdmin.createTopics({
            timeout: this.requestTimeout,
            topics: [{
                topic: topic,
                numPartitions: partitions,
                replicationFactor: this.replicationFactor,
                configEntries: this.configEntries
            }]
        });
    }

    private sendLoopWithLinger() {
        if (this.sendLoopInstance) {
            clearTimeout(this.sendLoopInstance);
        // } else {
        //     this.logger.debug("Starting new send loop with linger [%s]", this.linger)
        }
        this.sendLoopInstance = setTimeout(async () => {
            await this.sendMessagesAsBatch()
        }, this.linger);
    }

    async destroy(): Promise<void> {
        this.logger.info('Stopping Kafka resources...');

        if (this.kafkaAdmin) {
            this.logger.info('Stopping Kafka Admin...');
            const _kafkaAdmin = this.kafkaAdmin;
            // @ts-ignore
            delete this.kafkaAdmin;
            await _kafkaAdmin.disconnect();
            this.logger.info('Kafka Admin stopped.');
        }

        if (this.consumer) {
            this.logger.info('Stopping Kafka Consumer...');
            try {
                const _consumer = this.consumer;
                // @ts-ignore
                delete this.consumer;
                await _consumer.disconnect();
                this.logger.info('Kafka Consumer stopped.');
                await this.disconnectProducer();
            } catch (e: any) {
                this.logger.info('Kafka Consumer stop error.');
                await this.disconnectProducer();
            }
        }
        this.logger.info('Kafka resources stopped.');
        exit(0); //same as in version before
    }

    private async disconnectProducer(): Promise<void> {
        if (this.producer) {
            this.logger.info('Stopping Kafka Producer...');
            try {
                this.logger.info('Stopping loop...');
                clearTimeout(this.sendLoopInstance);
                await this.sendMessagesAsBatch();
                const _producer = this.producer;
                // @ts-ignore
                delete this.producer;
                await _producer.disconnect();
                this.logger.info('Kafka Producer stopped.');
            } catch (e) {
                this.logger.info('Kafka Producer stop error.');
            }
        }
    }

}
