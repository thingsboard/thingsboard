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

import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import {
  checkBoxCell,
  DateEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { DialogService } from '@core/services/dialog.service';
import { DeviceProfile, deviceProfileTypeTranslationMap } from '@shared/models/device.models';
import { DeviceProfileService } from '@core/http/device-profile.service';
import { DeviceProfileComponent } from '../../components/profile/device-profile.component';
import { DeviceProfileTabsComponent } from './device-profile-tabs.component';

@Injectable()
export class DeviceProfilesTableConfigResolver implements Resolve<EntityTableConfig<DeviceProfile>> {

  private readonly config: EntityTableConfig<DeviceProfile> = new EntityTableConfig<DeviceProfile>();

  constructor(private deviceProfileService: DeviceProfileService,
              private translate: TranslateService,
              private datePipe: DatePipe,
              private dialogService: DialogService) {

    this.config.entityType = EntityType.DEVICE_PROFILE;
    this.config.entityComponent = DeviceProfileComponent;
    this.config.entityTabsComponent = DeviceProfileTabsComponent;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DEVICE_PROFILE);
    this.config.entityResources = entityTypeResources.get(EntityType.DEVICE_PROFILE);

    this.config.columns.push(
      new DateEntityTableColumn<DeviceProfile>('createdTime', 'common.created-time', this.datePipe, '150px'),
      new EntityTableColumn<DeviceProfile>('name', 'device-profile.name', '20%'),
      new EntityTableColumn<DeviceProfile>('type', 'device-profile.type', '20%', (deviceProfile) => {
        return this.translate.instant(deviceProfileTypeTranslationMap.get(deviceProfile.type));
      }),
      new EntityTableColumn<DeviceProfile>('description', 'device-profile.description', '60%'),
      new EntityTableColumn<DeviceProfile>('isDefault', 'device-profile.default', '60px',
        entity => {
          return checkBoxCell(entity.default);
        })
    );

    this.config.cellActionDescriptors.push(
      {
        name: this.translate.instant('device-profile.set-default'),
        icon: 'flag',
        isEnabled: (deviceProfile) => !deviceProfile.default,
        onAction: ($event, entity) => this.setDefaultDeviceProfile($event, entity)
      }
    );

    this.config.deleteEntityTitle = deviceProfile => this.translate.instant('device-profile.delete-device-profile-title',
      { deviceProfileName: deviceProfile.name });
    this.config.deleteEntityContent = () => this.translate.instant('device-profile.delete-device-profile-text');
    this.config.deleteEntitiesTitle = count => this.translate.instant('device-profile.delete-device-profiles-title', {count});
    this.config.deleteEntitiesContent = () => this.translate.instant('device-profile.delete-device-profiles-text');

    this.config.entitiesFetchFunction = pageLink => this.deviceProfileService.getDeviceProfiles(pageLink);
    this.config.loadEntity = id => this.deviceProfileService.getDeviceProfile(id.id);
    this.config.saveEntity = deviceProfile => this.deviceProfileService.saveDeviceProfile(deviceProfile);
    this.config.deleteEntity = id => this.deviceProfileService.deleteDeviceProfile(id.id);
    this.config.onEntityAction = action => this.onDeviceProfileAction(action);
    this.config.deleteEnabled = (deviceProfile) => deviceProfile && !deviceProfile.default;
    this.config.entitySelectionEnabled = (deviceProfile) => deviceProfile && !deviceProfile.default;
  }

  resolve(): EntityTableConfig<DeviceProfile> {
    this.config.tableTitle = this.translate.instant('device-profile.device-profiles');

    return this.config;
  }

  setDefaultDeviceProfile($event: Event, deviceProfile: DeviceProfile) {
    if ($event) {
      $event.stopPropagation();
    }
    this.dialogService.confirm(
      this.translate.instant('device-profile.set-default-device-profile-title', {deviceProfileName: deviceProfile.name}),
      this.translate.instant('device-profile.set-default-device-profile-text'),
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'),
      true
    ).subscribe((res) => {
        if (res) {
          this.deviceProfileService.setDefaultDeviceProfile(deviceProfile.id.id).subscribe(
            () => {
              this.config.table.updateData();
            }
          );
        }
      }
    );
  }

  onDeviceProfileAction(action: EntityAction<DeviceProfile>): boolean {
    switch (action.action) {
      case 'setDefault':
        this.setDefaultDeviceProfile(action.event, action.entity);
        return true;
    }
    return false;
  }

}
