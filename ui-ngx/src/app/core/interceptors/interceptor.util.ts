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
