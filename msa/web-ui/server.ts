///
/// Copyright © 2016-2022 The Thingsboard Authors
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

import express from 'express';
import config from 'config';
import { _logger}  from './config/logger';
import path from 'path';
import http, { ServerResponse } from 'http';
import httpProxy from 'http-proxy';
import compression from 'compression';
import historyApiFallback from 'connect-history-api-fallback';

const logger = _logger('main');

let server: http.Server | null;

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
            app.all('/api/*', (req, res) => {
              logger.debug(req.method + ' ' + req.originalUrl);
              apiProxy.web(req, res);
            });

            app.all('/static/rulenode/*', (req, res) => {
              apiProxy.web(req, res);
            });

            server.on('upgrade', (req, socket, head) => {
              apiProxy.ws(req, socket, head);
            });
        }

        app.use(historyApiFallback());
        app.use(compression());

        const root = path.join(webDir, 'public');

        app.use(express.static(root));

        server.listen(bindPort, bindAddress, () => {
            logger.info('==> 🌎  Listening on port %s.', bindPort);
            logger.info('Started ThingsBoard Web UI Microservice.');
        }).on('error', (error) => {
            logger.error('Failed to start ThingsBoard Web UI Microservice: %s', error.message);
            logger.error(error.stack);
            exit(-1);
        });

    } catch (e: any) {
        logger.error('Failed to start ThingsBoard Web UI Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

process.on('exit', () => {
    exit(0);
});

function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    if (server) {
        logger.info('Stopping HTTP Server...');
        const _server = server;
        server = null;
        _server.close(() => {
            logger.info('HTTP Server stopped.');
            process.exit(status);
        });
    } else {
        process.exit(status);
    }
}
