// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { InterceptorHttpParams } from '../interceptors/interceptor-http-params';
import { HttpHeaders, HttpParams } from '@angular/common/http';
import { InterceptorConfig } from '../interceptors/interceptor-config';

export type QueryParams = { [param:string]: any };

export interface RequestConfig {
  ignoreLoading?: boolean;
  ignoreErrors?: boolean;
  resendRequest?: boolean;
  queryParams?: QueryParams;
}

export interface HttpOptionsResult {
  headers: HttpHeaders;
  params: HttpParams;
}

export interface HttpUploadOptionsResult {
  params: HttpParams;
}

export function hasRequestConfig(config?: any): boolean {
  if (!config) {
    return false;
  }
  return config.hasOwnProperty('ignoreLoading') || config.hasOwnProperty('ignoreErrors') || config.hasOwnProperty('resendRequest') || config.hasOwnProperty('queryParams');
}

export function createDefaultHttpOptions(queryParamsOrConfig?: QueryParams | RequestConfig, config?: RequestConfig) {
  if (hasRequestConfig(queryParamsOrConfig)) {
    return defaultHttpOptionsFromConfig(queryParamsOrConfig as RequestConfig);
  }
  return defaultHttpOptionsFromParams(queryParamsOrConfig as QueryParams, config);
}

export function defaultHttpOptionsFromParams(queryParams?: QueryParams, config?: RequestConfig) {
  const finalConfig = {
    ...config,
    ...(queryParams && { queryParams }),
  };
  return defaultHttpOptionsFromConfig(finalConfig);
}

export function defaultHttpOptionsFromConfig(config?: RequestConfig) {
  if (!config) {
    config = {};
  }
  return defaultHttpOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest, config.queryParams);
}

export function defaultHttpOptions(ignoreLoading: boolean = false,
                                   ignoreErrors: boolean = false,
                                   resendRequest: boolean = false,
                                   queryParams?: QueryParams) {
  const cleanedParams = cleanQueryParams(queryParams);

  return {
    headers: new HttpHeaders({'Content-Type': 'application/json'}),
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest), cleanedParams)
  };
}

export function defaultHttpUploadOptions(ignoreLoading: boolean = false,
                                         ignoreErrors: boolean = false,
                                         resendRequest: boolean = false,
                                         queryParams?: QueryParams) {
  const cleanedParams = cleanQueryParams(queryParams);

  return {
    params: new InterceptorHttpParams(new InterceptorConfig(ignoreLoading, ignoreErrors, resendRequest), cleanedParams)
  };
}

function cleanQueryParams(params?: QueryParams): QueryParams | undefined {
  if (!params) {
    return undefined;
  }

  const entries = Object.entries(params);

  const cleanedEntries = entries.filter(
    ([_, value]) => value !== null && value !== undefined
  );

  if (!cleanedEntries.length) {
    return undefined;
  }

  return Object.fromEntries(cleanedEntries);
}
