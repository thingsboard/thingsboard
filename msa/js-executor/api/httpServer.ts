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

import express from 'express';
import { _logger}  from '../config/logger';
import http from 'http';
import { Socket } from 'net';

export class HttpServer {

  private logger = _logger('httpServer');
  private app = express();
  private server: http.Server | null;
  private connections: Socket[] = [];

  constructor(httpPort: number) {
    this.app.get('/livenessProbe', async (req, res) => {
      const message = {
        now: new Date().toISOString()
      };
      res.send(message);
    })

    this.server = this.app.listen(httpPort, () => {
      this.logger.info('Started HTTP endpoint on port %s. Please, use /livenessProbe !', httpPort);
    }).on('error', (error) => {
      this.logger.error(error);
    });

    this.server.on('connection', connection => {
      this.connections.push(connection);
      connection.on('close', () => this.connections = this.connections.filter(curr => curr !== connection));
    });
  }

  async stop() {
    if (this.server) {
      this.logger.info('Stopping HTTP Server...');
      const _server = this.server;
      this.server = null;
      this.connections.forEach(curr => curr.end(() => curr.destroy()));
      await new Promise<void>(
          (resolve, reject) => {
            _server.close((err) => {
              this.logger.info('HTTP Server stopped.');
              resolve();
            });
          }
      );
    }
  }
}
