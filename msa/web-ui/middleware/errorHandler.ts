// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ErrorRequestHandler, Response } from 'express';
import { STATUS_CODES } from 'http';
import { _logger } from '../config/logger';

const logger = _logger('errorHandler');

const contentHeaders = ['content-encoding', 'content-language', 'content-range'];
// the body is plain text, so an error may bring back Content-Range (416) but never these
const deniedErrorHeaders = ['content-encoding', 'content-language'];

// express's default handler console.error()s every error, which lets any client flood stderr with 412/416 stack traces
export const errorHandler: ErrorRequestHandler = (err, req, res, _next) => {
    const status = errorStatus(err);
    if (status >= 500) {
        logger.error('%s %s failed: %s', req.method, req.originalUrl, err?.stack ?? err);
    }
    if (res.headersSent) {
        req.socket.destroy();
        return;
    }
    contentHeaders.forEach(name => res.removeHeader(name));
    if (status < 500) {
        setErrorHeaders(res, err?.headers);
    }
    res.status(status).type('text/plain').send(STATUS_CODES[status] ?? 'Error');
};

function errorStatus(err: any): number {
    for (const candidate of [err?.status, err?.statusCode]) {
        const status = Number(candidate);
        if (status >= 400 && status < 600) {
            return status;
        }
    }
    return 500;
}

function setErrorHeaders(res: Response, headers: unknown) {
    if (!headers || typeof headers !== 'object') {
        return;
    }
    for (const [name, value] of Object.entries(headers)) {
        if (!deniedErrorHeaders.includes(name.toLowerCase())) {
            res.setHeader(name, value as string | string[]);
        }
    }
}
