// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import config from 'config';
import path from 'path';
import DailyRotateFile from 'winston-daily-rotate-file';

import { LogEntry, logLevel } from 'kafkajs';
import { createLogger, format, transports } from 'winston';
import * as Transport from 'winston-transport';

const { combine, timestamp, label, printf, splat } = format;

const toWinstonLogLevel = (level: logLevel): string => {
    switch(level) {
        case logLevel.ERROR:
        case logLevel.NOTHING:
            return 'error'
        case logLevel.WARN:
            return 'warn'
        case logLevel.INFO:
            return 'info'
        case logLevel.DEBUG:
            return 'debug'
    }
}

const loggerTransports: Array<Transport> = [];

if (process.env.NODE_ENV !== 'production' || process.env.DOCKER_MODE === 'true') {
    loggerTransports.push(new transports.Console({
        handleExceptions: true
    }));
} else {
    const filename = path.join(config.get('logger.path'), config.get('logger.filename'));
    const transport = new (DailyRotateFile)({
        filename: filename,
        datePattern: 'YYYY-MM-DD-HH',
        zippedArchive: true,
        maxSize: '20m',
        maxFiles: '14d',
        handleExceptions: true
    });
    loggerTransports.push(transport);
}

const tbFormat = printf(info => {
    return `${info.timestamp} [${info.label}] ${info.level.toUpperCase()}: ${info.message}`;
});

export function _logger(moduleLabel: string) {
    return createLogger({
        level: config.get('logger.level'),
        format:combine(
            splat(),
            label({ label: moduleLabel }),
            timestamp({format: 'YYYY-MM-DD HH:mm:ss,SSS'}),
            tbFormat
        ),
        transports: loggerTransports
    });
}

export function KafkaJsWinstonLogCreator(logLevel: logLevel): (entry: LogEntry) => void {
    const logger = createLogger({
        level: toWinstonLogLevel(logLevel),
        format:combine(
            splat(),
            label({ label: 'kafkajs' }),
            timestamp({format: 'YYYY-MM-DD HH:mm:ss,SSS'}),
            printf(info => {
                var res = `${info.timestamp} [${info.label}] ${info.level.toUpperCase()}: ${info.message}`;
                if (info.extra) {
                    res +=`: ${JSON.stringify(info.extra)}`;
                }
                return res;
              }
            )
        ),
        transports: loggerTransports
    });

    return ({ namespace, level, label, log }) => {
        const { message, ...extra } = log;
        logger.log({
            level: toWinstonLogLevel(level),
            message,
            extra,
        });
    }
}
