// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { DeviceInfo, DeviceInfoFilter } from '@app/shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceProfileId } from '../../../../shared/models/id/device-profile-id';

@Component({
    selector: 'tb-device-table-header',
    templateUrl: './device-table-header.component.html',
    styleUrls: [],
    standalone: false
})
export class DeviceTableHeaderComponent extends EntityTableHeaderComponent<DeviceInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  deviceInfoFilterChanged(deviceInfoFilter: DeviceInfoFilter) {
    this.entitiesTableConfig.componentsData.deviceInfoFilter = deviceInfoFilter;
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

}
