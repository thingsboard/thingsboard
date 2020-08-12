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
const {logLevel, Kafka} = require('kafkajs');

const config = require('config'),
    JsInvokeMessageProcessor = require('../api/jsInvokeMessageProcessor'),
    logger = require('../config/logger')._logger('kafkaTemplate'),
    KafkaJsWinstonLogCreator = require('../config/logger').KafkaJsWinstonLogCreator;
const replicationFactor = config.get('kafka.replication_factor');
const topicProperties = config.get('kafka.topic_properties');

let kafkaClient;
let kafkaAdmin;
let consumer;
let producer;

const topics = [];
const configEntries = [];

function KafkaProducer() {
    this.send = async (responseTopic, scriptId, rawResponse, headers) => {

        if (!topics.includes(responseTopic)) {
            let createResponseTopicResult = await createTopic(responseTopic);
            topics.push(responseTopic);
            if (createResponseTopicResult) {
                logger.info('Created new topic: %s', requestTopic);
            }
        }

        return producer.send(
            {
                topic: responseTopic,
                messages: [
                    {
                        key: scriptId,
                        value: rawResponse,
                        headers: headers.data
                    }
                ]
            });
    }
}

(async () => {
    try {
        logger.info('Starting ThingsBoard JavaScript Executor Microservice...');

        const kafkaBootstrapServers = config.get('kafka.bootstrap.servers');
        const requestTopic = config.get('request_topic');
        const useConfluent = config.get('kafka.use_confluent_cloud');

        logger.info('Kafka Bootstrap Servers: %s', kafkaBootstrapServers);
        logger.info('Kafka Requests Topic: %s', requestTopic);

        let kafkaConfig = {
            brokers: kafkaBootstrapServers.split(','),
                logLevel: logLevel.INFO,
                logCreator: KafkaJsWinstonLogCreator
        };

        if (useConfluent) {
            kafkaConfig['sasl'] = {
                mechanism: config.get('kafka.confluent.sasl.mechanism'),
                username: config.get('kafka.confluent.username'),
                password: config.get('kafka.confluent.password')
            };
            kafkaConfig['ssl'] = true;
        }

        kafkaClient = new Kafka(kafkaConfig);

        parseTopicProperties();

        kafkaAdmin = kafkaClient.admin();
        await kafkaAdmin.connect();

        let createRequestTopicResult = await createTopic(requestTopic);

        if (createRequestTopicResult) {
            logger.info('Created new topic: %s', requestTopic);
        }

        consumer = kafkaClient.consumer({groupId: 'js-executor-group'});
        producer = kafkaClient.producer();
        const messageProcessor = new JsInvokeMessageProcessor(new KafkaProducer());
        await consumer.connect();
        await producer.connect();
        await consumer.subscribe({topic: requestTopic});

        logger.info('Started ThingsBoard JavaScript Executor Microservice.');
        await consumer.run({
            eachMessage: async ({topic, partition, message}) => {
                let headers = message.headers;
                let key = message.key;
                let msg = {};
                msg.key = key.toString('utf8');
                msg.data = message.value;
                msg.headers = {data: headers};
                messageProcessor.onJsInvokeMessage(msg);
            },
        });

    } catch (e) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

function createTopic(topic) {
    return kafkaAdmin.createTopics({
        topics: [{
            topic: topic,
            replicationFactor: replicationFactor,
            configEntries: configEntries
        }]
    });
}

function parseTopicProperties() {
    const props = topicProperties.split(';');
    props.forEach(p => {
        const delimiterPosition = p.indexOf(':');
        configEntries.push({name: p.substring(0, delimiterPosition), value: p.substring(delimiterPosition + 1)});
    });
}

process.on('exit', () => {
    exit(0);
});

async function exit(status) {
    logger.info('Exiting with status: %d ...', status);

    if (kafkaAdmin) {
        logger.info('Stopping Kafka Admin...');
        await kafkaAdmin.disconnect();
        logger.info('Kafka Admin stopped.');
    }

    if (consumer) {
        logger.info('Stopping Kafka Consumer...');
        let _consumer = consumer;
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
