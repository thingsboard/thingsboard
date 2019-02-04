/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
const config = require('config'),
      kafka = require('kafka-node'),
      ConsumerGroup = kafka.ConsumerGroup,
      Producer = kafka.Producer,
      JsInvokeMessageProcessor = require('./api/jsInvokeMessageProcessor'),
      logger = require('./config/logger')('main');

var kafkaClient;

(async() => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
        const kafkaRequestTopic = config.get('kafka.request_topic');

        logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        logger.info('Kafka Requests Topic: %s', kafkaRequestTopic);

        kafkaClient = new kafka.KafkaClient({kafkaHost: kafkaBootstrapServers});

        var consumer = new ConsumerGroup(
            {
                kafkaHost: kafkaBootstrapServers,
                groupId: 'js-executor-group',
                autoCommit: true,
                encoding: 'buffer'
            },
            kafkaRequestTopic
        );

        consumer.on('error', (err) => {
            logger.error('Unexpected kafka consumer error: %s', err.message);
            logger.error(err.stack);
        });

        consumer.on('offsetOutOfRange', (err) => {
            logger.error('Offset out of range error: %s', err.message);
            logger.error(err.stack);
        });

        consumer.on('rebalancing', () => {
            logger.info('Rebalancing event received.');
        })

        consumer.on('rebalanced', () => {
            logger.info('Rebalanced event received.');
        });

        var producer = new Producer(kafkaClient);
        producer.on('error', (err) => {
            logger.error('Unexpected kafka producer error: %s', err.message);
            logger.error(err.stack);
        });

        var messageProcessor = new JsInvokeMessageProcessor(producer);

        producer.on('ready', () => {
            consumer.on('message', (message) => {
                messageProcessor.onJsInvokeMessage(message);
            });
            logger.info('Started ThingsBoard JavaScript Executor Microservice.');
        });

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
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
        var _kafkaClient = kafkaClient;
        kafkaClient = null;
        _kafkaClient.close(() => {
            logger.info('Kafka Client stopped.');
            process.exit(status);
        });
    } else {
        process.exit(status);
    }
}
