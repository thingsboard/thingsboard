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
var config = require('config'),
    kafka = require('kafka-node'),
    Consumer = kafka.Consumer,
    Producer = kafka.Producer,
    JsMessageConsumer = require('./api/jsMessageConsumer');

var logger = require('./config/logger')('main');

var kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
var kafkaRequestTopic = config.get('kafka.request_topic');

logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
logger.info('Kafka Requests Topic: %s', kafkaRequestTopic);

var kafkaClient;

(async() => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        kafkaClient = new kafka.KafkaClient({kafkaHost: kafkaBootstrapServers});

        var consumer = new Consumer(
            kafkaClient,
            [
                { topic: kafkaRequestTopic, partition: 0 }
            ],
            {
                autoCommit: true,
                encoding: 'buffer'
            }
        );

        var producer = new Producer(kafkaClient);
        producer.on('error', (err) => {
            logger.error('Unexpected kafka producer error');
            logger.error(err);
        });

        producer.on('ready', () => {
            consumer.on('message', (message) => {
                JsMessageConsumer.onJsInvokeMessage(message, producer);
            });
        });

        logger.info('Started ThingsBoard JavaScript Executor Microservice.');
    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e);
        exit(-1);
    }
})();

process.on('exit', function () {
    exit(0);
});

function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (kafkaClient) {
        logger.info('Stopping Kafka Client...');
        kafkaClient.close(() => {
            logger.info('Kafka Client stopped.');
            process.exit(status);
        });
    } else {
        process.exit(status);
    }
}