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
import { _logger}  from '../config/logger';

export class HttpServer {

  private logger = _logger('httpServer');
  private app = express();
  private server;

  constructor(httpPort: number) {
    this.app.get('/livenessProbe', async (req, res) => {
      const message = {
        now: new Date().toISOString()
      };
      res.send(message);
    })

    this.server = this.app.listen(httpPort, () => {
      this.logger.info('Started http endpoint on port %s. Please, use /livenessProbe !', httpPort);
    }).on('error', (error) => {
      this.logger.error(error);
    });
  }

  stop() {
    this.server.close(() => {
      this.logger.info('Http server stop');
    });
  }
}
