///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig } from '@core/http/http-utils';
import { ConnectorConfigValidation } from '../models/configuration-validate.models';

@Injectable()
export class ConfigurationValidateService {

  constructor(private http: HttpClient) {}

  checkConnectorConfiguration(
    gatewayDeviceId: string,
    connectorType: string,
    connectorConfig: string
  ): Observable<ConnectorConfigValidation> {
    const url = `/api/gateway/${gatewayDeviceId}/configuration/${connectorType}/validate`;
    return this.http.post(
      url,
      connectorConfig,
      defaultHttpOptionsFromConfig({ignoreErrors: true})
    ) as Observable<ConnectorConfigValidation>;
  }
}
