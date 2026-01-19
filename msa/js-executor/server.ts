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
import { _logger } from './config/logger';
import { HttpServer } from './api/httpServer';
import { IQueue } from './queue/queue.models';
import { KafkaTemplate } from './queue/kafkaTemplate';

const logger = _logger('main');

logger.info('===CONFIG BEGIN===');
logger.info(JSON.stringify(config, null, 4));
logger.info('===CONFIG END===');

const serviceType: string = config.get('queue_type');
const httpPort = Number(config.get('http_port'));
let queues: IQueue | null;
let httpServer: HttpServer | null;

(async () => {
    logger.info('Starting ThingsBoard JavaScript Executor Microservice...');
    try {
        queues = await createQueue(serviceType);
        logger.info(`Starting ${queues.name} template...`);
        await queues.init();
        logger.info(`${queues.name} template started.`);
        httpServer = new HttpServer(httpPort);
    } catch (e: any) {
        logger.error('Failed to start ThingsBoard JavaScript Executor Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }

})();

async function createQueue(serviceType: string): Promise<IQueue> {
    switch (serviceType) {
        case 'kafka':
            return new KafkaTemplate();
        default:
            throw new Error('Unknown service type: ' + serviceType);
    }
}

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    })
})

process.on('exit', (code: number) => {
    logger.info(`ThingsBoard JavaScript Executor Microservice has been stopped. Exit code: ${code}.`);
});

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    try {
        if (httpServer) {
            const _httpServer = httpServer;
            httpServer = null;
            await _httpServer.stop();
        }
        if (queues) {
            const _queues = queues;
            queues = null;
            await _queues.destroy();
        }
    } catch (e) {
        logger.error('Error on exit');
    }
    process.exit(status);
}
