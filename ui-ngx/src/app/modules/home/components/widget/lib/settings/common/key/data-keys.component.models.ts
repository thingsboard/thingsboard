// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { DataKey } from '@shared/models/widget.models';
import { Observable } from 'rxjs';
import { FormProperty } from '@shared/models/dynamic-form.models';

export type DataKeySettingsFormFunction = (key: DataKey) => FormProperty[];
export type DataKeySettingsFunction = (key: DataKey, isLatestDataKey: boolean) => any;

export interface DataKeysCallbacks {
  generateDataKey: (chip: any, type: DataKeyType, dataKeySettingsForm: FormProperty[],
                    isLatestDataKey: boolean, dataKeySettingsFunction: DataKeySettingsFunction) => DataKey;
  fetchEntityKeys: (entityAliasId: string, types: Array<DataKeyType>) => Observable<Array<DataKey>>;
  fetchEntityKeysForDevice: (deviceId: string, types: Array<DataKeyType>) => Observable<Array<DataKey>>;
}
