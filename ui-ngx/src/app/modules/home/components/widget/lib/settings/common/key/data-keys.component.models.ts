///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { FormProperty } from '@shared/models/dynamic-form.models';

export type DataKeySettingsFunction = (key: DataKey, isLatestDataKey: boolean) => any;

export interface DataKeysCallbacks {
  generateDataKey: (chip: any, type: DataKeyType, dataKeySettingsForm: FormProperty[],
                    isLatestDataKey: boolean, dataKeySettingsFunction: DataKeySettingsFunction) => DataKey;
  fetchEntityKeys: (entityAliasId: string, types: Array<DataKeyType>) => Observable<Array<DataKey>>;
  fetchEntityKeysForDevice: (deviceId: string, types: Array<DataKeyType>) => Observable<Array<DataKey>>;
}
