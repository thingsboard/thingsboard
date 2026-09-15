// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
