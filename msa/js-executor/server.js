/*
 * Copyright © 2016-2021 The Thingsboard Authors
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
const config = require('config'), logger = require('./config/logger')._logger('main');

logger.info('===CONFIG BEGIN===');
logger.info(JSON.stringify(config, null, 4));
logger.info('===CONFIG END===');

const serviceType = config.get('queue_type');
switch (serviceType) {
    case 'kafka':
        logger.info('Starting kafka template.');
        require('./queue/kafkaTemplate');
        logger.info('kafka template started.');
        break;
    case 'pubsub':
        logger.info('Starting Pub/Sub template.')
        require('./queue/pubSubTemplate');
        logger.info('Pub/Sub template started.')
        break;
    case 'aws-sqs':
        logger.info('Starting Aws Sqs template.')
        require('./queue/awsSqsTemplate');
        logger.info('Aws Sqs template started.')
        break;
    case 'rabbitmq':
        logger.info('Starting RabbitMq template.')
        require('./queue/rabbitmqTemplate');
        logger.info('RabbitMq template started.')
        break;
    case 'service-bus':
        logger.info('Starting Azure Service Bus template.')
        require('./queue/serviceBusTemplate');
        logger.info('Azure Service Bus template started.')
        break;
    default:
        logger.error('Unknown service type: ', serviceType);
        process.exit(-1);
}

