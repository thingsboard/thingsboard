// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import express from 'express';
import config from 'config';
import { _logger}  from './config/logger';
import path from 'path';
import http, { ServerResponse } from 'http';
import httpProxy from 'http-proxy';
import fs from 'fs';
import historyApiFallback from 'connect-history-api-fallback';
import { Socket } from 'net';
import Negotiator from 'negotiator';
import { RequestHandler } from 'express';

const logger = _logger('main');

let server: http.Server | null;
let connections: Socket[] = [];

// in server preference order; must match the encoders in ui-ngx/compress-build.js
const precompressedEncodings = [
    {encoding: 'br', ext: '.br'},
    {encoding: 'gzip', ext: '.gz'}
];

interface PrecompressedVariant {
    encoding: string;
    ext: string;
    files: Set<string>;
}

(async() => {
    try {
        logger.info('Starting ThingsBoard Web UI Microservice...');

        const bindAddress: string = config.get('server.address');
        const bindPort = Number(config.get('server.port'));

        const thingsboardEnableProxy: string = config.get('thingsboard.enableProxy');

        const thingsboardHost: string = config.get('thingsboard.host');
        const thingsboardPort = Number(config.get('thingsboard.port'));

        logger.info('Bind address: %s', bindAddress);
        logger.info('Bind port: %s', bindPort);
        logger.info('ThingsBoard Enable Proxy: %s', thingsboardEnableProxy);
        logger.info('ThingsBoard host: %s', thingsboardHost);
        logger.info('ThingsBoard port: %s', thingsboardPort);

        const useApiProxy = thingsboardEnableProxy === "true";

        let webDir = path.join(__dirname, 'web');

        if (typeof process.env.WEB_FOLDER !== 'undefined') {
            webDir = path.resolve(process.env.WEB_FOLDER);
        }
        logger.info('Web folder: %s', webDir);

        const app = express();
        server = http.createServer(app);

        // Build security headers map once at startup.
        // node-config passes env var overrides as strings, so enabled can be boolean or string.
        const isEnabled = (val: any) => val === true || val === 'true';
        const securityHeaders: Record<string, string> = {};
        const hc: any = config.get('security.headers');
        if (isEnabled(hc['x-content-type-options']?.enabled)) {
            securityHeaders['X-Content-Type-Options'] = 'nosniff';
        }
        if (isEnabled(hc['referrer-policy']?.enabled)) {
            securityHeaders['Referrer-Policy'] = hc['referrer-policy']?.value || 'strict-origin-when-cross-origin';
        }
        if (isEnabled(hc['x-frame-options']?.enabled)) {
            securityHeaders['X-Frame-Options'] = hc['x-frame-options']?.value || 'SAMEORIGIN';
        }
        if (isEnabled(hc['content-security-policy']?.enabled) && hc['content-security-policy']?.value) {
            const csp = hc['content-security-policy'];
            const name = isEnabled(csp['report-only'])
                ? 'Content-Security-Policy-Report-Only' : 'Content-Security-Policy';
            securityHeaders[name] = csp.value;
        }
        logger.info('Security headers: %s', JSON.stringify(securityHeaders));

        // Apply security headers to all responses
        app.use((_req, res, next) => {
            for (const [name, value] of Object.entries(securityHeaders)) {
                res.setHeader(name, value);
            }
            next();
        });

        let apiProxy: httpProxy;
        if (useApiProxy) {
            apiProxy = httpProxy.createProxyServer({
                target: {
                    host: thingsboardHost,
                    port: thingsboardPort
                }
            });

            apiProxy.on('error', (err, req, res) => {
                logger.warn('API proxy error: %s', err.message);
                if (res instanceof ServerResponse) {
                    res.writeHead(500);
                    const error = err as any;
                    if (error.code && error.code === 'ECONNREFUSED') {
                        res.end('Unable to connect to ThingsBoard server.');
                    } else {
                        res.end('ThingsBoard server connection error: ' + error.code ? error.code : '');
                    }
                }
            });
            app.all('/api/*splat', (req, res) => {
              logger.debug(req.method + ' ' + req.originalUrl);
              apiProxy.web(req, res);
            });

            app.all('/static/rulenode/*splat', (req, res) => {
              apiProxy.web(req, res);
            });

            server.on('upgrade', (req, socket, head) => {
              apiProxy.ws(req, socket, head);
            });
        }

        app.use(historyApiFallback());

        const root = path.join(webDir, 'public');

        // No per-request compression: zlib/brotli fragment native memory that glibc never returns, until OOM
        const variants = listPrecompressedFiles(root);
        if (variants.some(({files}) => files.size)) {
            logger.info('Pre-compressed files: %s', variants.map(({encoding, files}) => `${encoding} ${files.size}`).join(', '));
        } else {
            logger.warn('No pre-compressed (.br/.gz) UI files in %s: UI files are served uncompressed', root);
        }
        app.use(precompressedStatic(root, variants));

        const staticServe = express.static(root);

        const indexHtmlFallback: RequestHandler = (req, res, next) => {
          res.sendFile(path.join(root, 'index.html'));
        };

        app.use(staticServe);

        const indexHtmlFallbackPaths = [
          /^\/resources\/scada-symbols\/(?:system|tenant)\/[^/]+$/
        ];

        app.use(indexHtmlFallbackPaths, indexHtmlFallback);

        server.listen(bindPort, bindAddress, () => {
            logger.info('==> 🌎  Listening on port %s.', bindPort);
            logger.info('Started ThingsBoard Web UI Microservice.');
        }).on('error', async (error) => {
            logger.error('Failed to start ThingsBoard Web UI Microservice: %s', error.message);
            logger.error(error.stack);
            await exit(-1);
        });

        server.on('connection', connection => {
            connections.push(connection);
            connection.on('close', () => connections = connections.filter(curr => curr !== connection));
        });

    } catch (e: any) {
        logger.error('Failed to start ThingsBoard Web UI Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }
})();

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    });
})

process.on('exit', async (code: number) => {
    logger.info(`ThingsBoard Web UI Microservice has been stopped. Exit code: ${code}.`);
});

function listPrecompressedFiles(root: string): PrecompressedVariant[] {
    const variants = precompressedEncodings.map(encoding => ({...encoding, files: new Set<string>()}));
    if (fs.existsSync(root)) {
        for (const file of fs.readdirSync(root, {recursive: true, encoding: 'utf8'})) {
            const variant = variants.find(({ext}) => file.endsWith(ext));
            variant?.files.add('/' + file.slice(0, -variant.ext.length).split(path.sep).join('/'));
        }
    }
    return variants;
}

function precompressedStatic(root: string, variants: PrecompressedVariant[]): RequestHandler {
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
        const available = variants.filter(({files}) => files.has(filePath));
        if (!available.length) {
            return next();
        }
        res.vary('Accept-Encoding');
        // highest q wins; on a tie the server's order does, as browsers list gzip before br
        const encodings = available.map(({encoding}) => encoding);
        const best = new Negotiator(req).encoding([...encodings, 'identity'], {preferred: encodings});
        const variant = available.find(({encoding}) => encoding === best);
        if (!variant) {
            return next();
        }
        res.setHeader('Content-Encoding', variant.encoding);
        res.type(path.extname(filePath));
        res.sendFile(filePath + variant.ext, {root}, (err?: NodeJS.ErrnoException & { status?: number }) => {
            // the client went away; express.static ignores these too
            if (!err || err.code === 'ECONNABORTED' || err.syscall === 'write') {
                return;
            }
            if (res.headersSent) {
                return next(err);
            }
            res.removeHeader('Content-Encoding');
            // the copy was removed after startup: fall through to the original
            next(err.status === 404 ? undefined : err);
        });
    };
}

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    if (server) {
        logger.info('Stopping HTTP Server...');
        connections.forEach(curr => curr.end(() => curr.destroy()));
        const _server = server;
        server = null;
        const serverClosePromise = new Promise<void>(
            (resolve, reject) => {
                _server.close((err) => {
                    logger.info('HTTP Server stopped.');
                    resolve();
                });
            }
        );
        await serverClosePromise;
    }
    process.exit(status);
}
