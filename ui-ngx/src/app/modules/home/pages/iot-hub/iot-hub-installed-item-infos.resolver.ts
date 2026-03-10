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

import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { IotHubApiService } from '@core/http/iot-hub-api.service';
import { IotHubInstalledItemInfo } from '@shared/models/iot-hub/iot-hub-installed-item.models';

export const iotHubInstalledItemInfosResolver: ResolveFn<IotHubInstalledItemInfo[]> = () => {
  const iotHubApiService = inject(IotHubApiService);
  return iotHubApiService.getInstalledItemInfos({ ignoreLoading: true });
};
