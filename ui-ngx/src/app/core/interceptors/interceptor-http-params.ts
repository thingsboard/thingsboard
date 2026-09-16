// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { HttpParams } from '@angular/common/http';
import { InterceptorConfig } from './interceptor-config';

export class InterceptorHttpParams extends HttpParams {
  constructor(
    public interceptorConfig: InterceptorConfig,
    params?: { [param: string]: string | number | boolean | ReadonlyArray<string | number | boolean>; }
  ) {
    super({ fromObject: params });
  }
}
