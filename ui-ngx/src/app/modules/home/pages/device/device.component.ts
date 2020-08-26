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

import { Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DeviceInfo } from '@shared/models/device.models';
import { EntityType } from '@shared/models/entity-type.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { DeviceService } from '@core/http/device.service';
import { ClipboardService } from 'ngx-clipboard';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.scss']
})
export class DeviceComponent extends EntityComponent<DeviceInfo> {

  entityType = EntityType;

  deviceScope: 'tenant' | 'customer' | 'customer_user';

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private deviceService: DeviceService,
              private clipboardService: ClipboardService,
              @Inject('entity') protected entityValue: DeviceInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<DeviceInfo>,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  ngOnInit() {
    this.deviceScope = this.entitiesTableConfig.componentsData.deviceScope;
    super.ngOnInit();
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isAssignedToCustomer(entity: DeviceInfo): boolean {
    return entity && entity.customerId && entity.customerId.id !== NULL_UUID;
  }

  buildForm(entity: DeviceInfo): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        deviceProfileId: [entity ? entity.deviceProfileId : null, [Validators.required]],
        type: [entity ? entity.type : null, [Validators.required]],
        label: [entity ? entity.label : ''],
        deviceData: [entity ? entity.deviceData : null, [Validators.required]],
        additionalInfo: this.fb.group(
          {
            gateway: [entity && entity.additionalInfo ? entity.additionalInfo.gateway : false],
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: DeviceInfo) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({deviceProfileId: entity.deviceProfileId});
    this.entityForm.patchValue({type: entity.type});
    this.entityForm.patchValue({label: entity.label});
    this.entityForm.patchValue({deviceData: entity.deviceData});
    this.entityForm.patchValue({additionalInfo:
        {gateway: entity.additionalInfo ? entity.additionalInfo.gateway : false}});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onDeviceIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('device.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  copyAccessToken($event) {
    if (this.entity.id) {
      this.deviceService.getDeviceCredentials(this.entity.id.id, true).subscribe(
        (deviceCredentials) => {
          const credentialsId = deviceCredentials.credentialsId;
          if (this.clipboardService.copyFromContent(credentialsId)) {
            this.store.dispatch(new ActionNotificationShow(
              {
                message: this.translate.instant('device.accessTokenCopiedMessage'),
                type: 'success',
                duration: 750,
                verticalPosition: 'bottom',
                horizontalPosition: 'right'
              }));
          }
        }
      );
    }
  }
}
