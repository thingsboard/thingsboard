// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { InterceptorHttpParams } from '../interceptors/interceptor-http-params';
import { HttpHeaders } from '@angular/common/http';
import { InterceptorConfig } from '../interceptors/interceptor-config';

export interface RequestConfig {
  ignoreLoading?: boolean;
  ignoreErrors?: boolean;
  resendRequest?: boolean;
}

export function defaultHttpOptionsFromConfig(config?: RequestConfig) {
  if (!config) {
    config = {};
  }
  return defaultHttpOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest);
}

export function defaultHttpOptions(ignoreLoading: boolean = false,
                                   ignoreErrors: boolean = false,
                                   resendRequest: boolean = false) {
  return {
    headers: new HttpHeaders({'Content-Type': 'application/json'}),
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest))
  };
}

export function defaultHttpUploadOptions(ignoreLoading: boolean = false,
                                         ignoreErrors: boolean = false,
                                         resendRequest: boolean = false) {
  return {
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest))
  };
}
