// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
