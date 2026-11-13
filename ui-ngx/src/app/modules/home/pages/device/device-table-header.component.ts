// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableHeaderComponent } from '../../components/entity/entity-table-header.component';
import { Device, DeviceInfo, DeviceInfoFilter } from '@app/shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { DeviceProfileId } from '@shared/models/id/device-profile-id';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { TranslateService } from '@ngx-translate/core';
import { isDefinedAndNotNull } from '@core/utils';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'tb-device-table-header',
    templateUrl: './device-table-header.component.html',
    styleUrls: ['../asset/asset-table-header.component.scss'],
    standalone: false
})
export class DeviceTableHeaderComponent extends EntityTableHeaderComponent<DeviceInfo> implements OnInit {

  entityType = EntityType;

  includeCustomersLabel: string;

  private deviceProfileService = inject(DeviceProfileService);
  private translate = inject(TranslateService);

  readonly deviceInfoFilterLabel = (deviceInfoFilter: DeviceInfoFilter): Observable<string> => {
    const stateText = isDefinedAndNotNull(deviceInfoFilter.active)
      ? this.translate.instant(deviceInfoFilter.active ? 'device.active' : 'device.inactive') : null;
    const profileName$ = isDefinedAndNotNull(deviceInfoFilter.deviceProfileId)
      ? this.deviceProfileService.getDeviceProfileInfo(deviceInfoFilter.deviceProfileId.id, {ignoreLoading: true, ignoreErrors: true})
        .pipe(map(deviceProfileInfo => deviceProfileInfo.name))
      : of(null);
    return profileName$.pipe(map(profileName => [profileName, stateText].filter(Boolean).join(', ')));
  };

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.includeCustomersLabel = (getCurrentAuthUser(this.store).authority === Authority.CUSTOMER_USER ||
      this.entitiesTableConfig.customerId) ? 'entity.include-sub-customer-entities' : 'entity.include-customer-entities';
  }

  deviceInfoFilterChanged(deviceInfoFilter: DeviceInfoFilter) {
    this.entitiesTableConfig.componentsData.deviceInfoFilter = deviceInfoFilter ?? {};
    this.entitiesTableConfig.getTable().resetSortAndFilter(true);
  }

  includeCustomersChanged(includeCustomers: boolean) {
    this.entitiesTableConfig.componentsData.includeCustomersChanged(includeCustomers);
  }

}
