///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { DeviceInfo } from '@app/shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-device-table-header',
  templateUrl: './device-table-header.component.html',
  styleUrls: ['./device-table-header.component.scss']
})
export class DeviceTableHeaderComponent extends EntityTableHeaderComponent<DeviceInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  deviceTypeChanged(deviceType: string) {
    this.entitiesTableConfig.componentsData.deviceType = deviceType;
    this.entitiesTableConfig.table.resetSortAndFilter(true);
  }

}
