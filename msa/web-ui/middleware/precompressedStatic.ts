// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import express, { NextFunction, Request, RequestHandler, Response } from 'express';
import fs from 'fs';
import { IncomingMessage, OutgoingHttpHeaders } from 'http';
import path from 'path';
import { _logger } from '../config/logger';

interface EncodingNegotiator {
    encodings(available: readonly string[], options: { preferred: readonly string[] }): string[];
}

// negotiator 1.x ships no type definitions
const Negotiator: new (req: IncomingMessage) => EncodingNegotiator = require('negotiator');

interface Variant {
    encoding: string;
    ext: string;
    serve: RequestHandler;
}

interface ConfigSource {
    has(key: string): boolean;
    get(key: string): unknown;
}

const logger = _logger('precompressedStatic');

export const PRECOMPRESSED_ENCODINGS_KEY = 'server.compression.precompressed_encodings';

// must match the compressors of tb-compressor-plugin in ui-ngx/esbuild/tb-esbuild-plugins.ts
export const precompressedExtensions: Readonly<Record<string, string>> = {br: '.br', gzip: '.gz'};

export function precompressedEncodingsFromConfig(config: ConfigSource): string[] {
    return config.has(PRECOMPRESSED_ENCODINGS_KEY) ? parsePrecompressedEncodings(config.get(PRECOMPRESSED_ENCODINGS_KEY)) : [];
}

export function parsePrecompressedEncodings(value: unknown): string[] {
    const items = (Array.isArray(value) ? value : String(value ?? '').split(','))
        .map(item => String(item).trim().toLowerCase())
        .filter(item => item);
    const disabled = items.includes('none');
    const encodings = new Set<string>();
    for (const encoding of items) {
        if (encoding === 'none') {
            continue;
        }
        if (precompressedExtensions[encoding]) {
            encodings.add(encoding);
        } else {
            logger.warn('Ignoring unsupported pre-compressed file encoding: %s', encoding);
        }
    }
    if (!encodings.size && !disabled) {
        logger.warn('No supported pre-compressed file encodings in "%s" (use "none" to disable): pre-compressed files will not be served', value);
    }
    return [...encodings];
}

// resolution goes through express.static, so traversal/dotfile handling is unchanged
export function precompressedStatic(root: string, encodings: string[]): RequestHandler {
    const variants: Variant[] = encodings.map(encoding => {
        const ext = precompressedExtensions[encoding];
        const serve = express.static(root, {
            index: false,
            redirect: false,
            fallthrough: true,
            setHeaders: (res, filePath) => {
                res.setHeader('Content-Encoding', encoding);
                res.type(path.extname(filePath.slice(0, -ext.length)));
            }
        });
        return {encoding, ext, serve};
    });
    // the web folder is immutable at runtime, so index it once instead of probing the disk per request
    const available = listPrecompressedFiles(root, variants.map(variant => variant.ext));
    logger.info('Pre-compressed files found: %d', available.size);
    return (req, res, next) => {
        if (req.method !== 'GET' && req.method !== 'HEAD') {
            return next();
        }
        let filePath: string;
        try {
            filePath = decodeURIComponent(req.path);
        } catch {
            return next();
        }
        const candidates = variants.filter(variant => available.has(filePath + variant.ext));
        if (!candidates.length) {
            return next();
        }
        res.vary('Accept-Encoding');
        if (!req.headers['accept-encoding']) {
            return next();
        }
        serveFirst(negotiate(req, candidates), filePath, req, res, next);
    };
}

function negotiate(req: Request, candidates: Variant[]): Variant[] {
    const names = candidates.map(variant => variant.encoding);
    const ranked = new Negotiator(req).encodings([...names, 'identity'], {preferred: names});
    const identityIndex = ranked.indexOf('identity');
    return ranked.slice(0, identityIndex === -1 ? ranked.length : identityIndex)
        .flatMap(encoding => candidates.filter(variant => variant.encoding === encoding));
}

function serveFirst(variants: Variant[], filePath: string, req: Request, res: Response, next: NextFunction) {
    const originalUrl = req.url;
    const {pathname, query} = splitUrl(originalUrl);
    // a failed attempt may already have set its own Content-Encoding, ETag etc., so each retry starts from here
    const initialStatus = res.statusCode;
    const initialHeaders = res.getHeaders();
    const tryVariant = (index: number, err?: any) => {
        req.url = originalUrl;
        if (err) {
            if (res.headersSent || isClientError(err)) {
                return next(err);
            }
            logger.warn('Failed to serve pre-compressed file for %s: %s', filePath, err.message);
            restoreResponse(res, initialStatus, initialHeaders);
        }
        if (index >= variants.length) {
            return next();
        }
        req.url = pathname + variants[index].ext + query;
        variants[index].serve(req, res, (e?: any) => tryVariant(index + 1, e));
    };
    tryVariant(0);
}

// client errors (412, 416) are answers for this variant, not a reason to try the next one
function isClientError(err: any): boolean {
    return (err.status ?? err.statusCode ?? 500) < 500;
}

function splitUrl(url: string): { pathname: string, query: string } {
    const queryIndex = url.indexOf('?');
    return queryIndex === -1 ? {pathname: url, query: ''} : {pathname: url.slice(0, queryIndex), query: url.slice(queryIndex)};
}

function restoreResponse(res: Response, status: number, headers: OutgoingHttpHeaders) {
    for (const name of res.getHeaderNames()) {
        if (!(name in headers)) {
            res.removeHeader(name);
        }
    }
    for (const [name, value] of Object.entries(headers)) {
        res.setHeader(name, value!);
    }
    res.statusCode = status;
}

function listPrecompressedFiles(root: string, extensions: string[]): Set<string> {
    const files = new Set<string>();
    if (!extensions.length || !fs.existsSync(root)) {
        return files;
    }
    for (const file of fs.readdirSync(root, {recursive: true, encoding: 'utf8'})) {
        if (extensions.some(ext => file.endsWith(ext))) {
            files.add('/' + file.split(path.sep).join('/'));
        }
    }
    return files;
}
