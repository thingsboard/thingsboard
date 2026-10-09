// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Component, DestroyRef, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import {
  DeviceProfile,
  DeviceTransportType,
  deviceTransportTypeHintMap,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Operation, Resource } from "@shared/models/security.models";
import { UserPermissionsService } from "@core/http/user-permissions.service";

@Component({
    selector: 'tb-device-profile-tabs',
    templateUrl: './device-profile-tabs.component.html',
    styleUrls: [],
    standalone: false
})
export class DeviceProfileTabsComponent extends EntityTabsComponent<DeviceProfile> implements OnInit {

  deviceTransportTypes = Object.values(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceTransportTypeHints = deviceTransportTypeHintMap;

  isTransportTypeChanged = false;

  hasOldRules = false;
  alarmRulesOldVersion = false;

  constructor(protected store: Store<AppState>,
              private destroyRef: DestroyRef,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.detailsForm.get('transportType').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.isTransportTypeChanged = true;
    });
  }

  resolveTabIndex(tab: string): number {
    if (tab === 'cf') {
      return 2;
    } else {
      return super.resolveTabIndex(tab);
    }
  }

  protected setEntity(entity: DeviceProfile) {
    this.isTransportTypeChanged = false;
    this.hasOldRules = !!entity?.profileData?.alarms?.length;
    this.alarmRulesOldVersion = !this.userPermissionsService.hasGenericPermission(Resource.DEVICE_PROFILE, Operation.READ_CALCULATED_FIELD);
    super.setEntity(entity);
  }

}
