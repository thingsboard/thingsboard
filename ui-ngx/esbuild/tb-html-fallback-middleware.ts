// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import type { ServerResponse } from 'node:http';
import type { Connect } from 'vite';
import type { NextHandleFunction } from 'connect';

const tbHtmlFallbackMiddleware: NextHandleFunction = (
  req: Connect.IncomingMessage,
  _res: ServerResponse,
  next: Connect.NextFunction
) => {
  if (/^\/resources\/scada-symbols\/(?:system|tenant)\/[^/]+$/.test(req.url)) {
    req.url = '/';
  }
  next();
}

export default tbHtmlFallbackMiddleware;
