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
const { logLevel, Kafka } = require('kafkajs');

const config = require('config'),
      JsInvokeMessageProcessor = require('./api/jsInvokeMessageProcessor'),
      logger = require('./config/logger')._logger('main'),
      KafkaJsWinstonLogCreator = require('./config/logger').KafkaJsWinstonLogCreator;

var kafkaClient;
var consumer;
var producer;

(async() => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
        const kafkaRequestTopic = config.get('kafka.request_topic');

        logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        logger.info('Kafka Requests Topic: %s', kafkaRequestTopic);

        kafkaClient = new Kafka({
            brokers: kafkaBootstrapServers.split(','),
            logLevel: logLevel.INFO,
            logCreator: KafkaJsWinstonLogCreator
        });

        consumer = kafkaClient.consumer({ groupId: 'js-executor-group' });
        producer = kafkaClient.producer();
        const messageProcessor = new JsInvokeMessageProcessor(producer);
        await consumer.connect();
        await producer.connect();
        await consumer.subscribe({ topic: kafkaRequestTopic});

        logger.info('Started ThingsBoard JavaScript Executor Microservice.');
        await consumer.run({
            eachMessage: async ({ topic, partition, message }) => {
                messageProcessor.onJsInvokeMessage(message);
            },
        });

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
    if (consumer) {
        logger.info('Stopping Kafka Consumer...');
        var _consumer = consumer;
        consumer = null;
        try {
            await _consumer.disconnect();
            logger.info('Kafka Consumer stopped.');
            await disconnectProducer();
            process.exit(status);
        } catch (e) {
            logger.info('Kafka Consumer stop error.');
            await disconnectProducer();
            process.exit(status);
        }
    } else {
        process.exit(status);
    }
}

async function disconnectProducer() {
    if (producer) {
        logger.info('Stopping Kafka Producer...');
        var _producer = producer;
        producer = null;
        try {
            await _producer.disconnect();
            logger.info('Kafka Producer stopped.');
        } catch (e) {
            logger.info('Kafka Producer stop error.');
        }
    }
}
