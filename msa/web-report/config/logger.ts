// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import config from 'config';
import path from 'path';
import DailyRotateFile from 'winston-daily-rotate-file';
import { createLogger, format, transports }  from 'winston';
import * as Transport from 'winston-transport';
const { combine, timestamp, label, printf, splat } = format;

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
    let logMessage = `${info.timestamp} [${info.label}] ${info.level.toUpperCase()}: ${info.message}`;
    if (info.stack) {
        logMessage += ':\n' + info.stack;
    }
    return logMessage;
});

// Writes buffered messages to the log file and closes it. process.exit() drops them otherwise, so a startup error
// logged right before exiting would never reach the file.
export function flushLogs(timeoutMs = 5000): Promise<void> {
    const closing = loggerTransports
        .filter((transport): transport is DailyRotateFile => transport instanceof DailyRotateFile)
        .map(transport => new Promise<void>(resolve => {
            transport.once('finish', () => resolve());
            transport.close?.();
        }));
    const timeout = new Promise<void>(resolve => setTimeout(resolve, timeoutMs));
    return Promise.race([Promise.all(closing).then(() => undefined), timeout]);
}

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
