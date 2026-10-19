// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { HttpRequest } from '@angular/common/http';
import { InterceptorConfig } from '@core/interceptors/interceptor-config';
import { InterceptorHttpParams } from '@core/interceptors/interceptor-http-params';

const internalUrlPrefixes = [
  '/api/auth/token',
  '/api/rpc'
];

export const getInterceptorConfig = (req: HttpRequest<unknown>): InterceptorConfig => {
  let config: InterceptorConfig;
  if (req.params && req.params instanceof InterceptorHttpParams) {
    config = (req.params as InterceptorHttpParams).interceptorConfig;
  } else {
    config = new InterceptorConfig();
  }
  if (isInternalUrlPrefix(req.url)) {
    config.ignoreLoading = true;
  }
  return config;
};

const isInternalUrlPrefix = (url: string): boolean => {
  for (const prefix of internalUrlPrefixes) {
    if (url.startsWith(prefix)) {
      return true;
    }
  }
  return false;
};
