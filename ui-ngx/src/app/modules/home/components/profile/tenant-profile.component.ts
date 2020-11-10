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

import { Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import {
  createTenantProfileConfiguration,
  TenantProfile,
  TenantProfileData,
  TenantProfileType
} from '@shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';

@Component({
  selector: 'tb-tenant-profile',
  templateUrl: './tenant-profile.component.html',
  styleUrls: ['./tenant-profile.component.scss']
})
export class TenantProfileComponent extends EntityComponent<TenantProfile> {

  @Input()
  standalone = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: TenantProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantProfile>,
              protected fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: TenantProfile): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        isolatedTbCore: [entity ? entity.isolatedTbCore : false, []],
        isolatedTbRuleEngine: [entity ? entity.isolatedTbRuleEngine : false, []],
        profileData: [entity && !this.isAdd ? entity.profileData : {
          configuration: createTenantProfileConfiguration(TenantProfileType.DEFAULT)
        } as TenantProfileData, []],
        description: [entity ? entity.description : '', []],
      }
    );
  }

  updateForm(entity: TenantProfile) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({isolatedTbCore: entity.isolatedTbCore});
    this.entityForm.patchValue({isolatedTbRuleEngine: entity.isolatedTbRuleEngine});
    this.entityForm.patchValue({profileData: !this.isAdd ? entity.profileData : {
        configuration: createTenantProfileConfiguration(TenantProfileType.DEFAULT)
      } as TenantProfileData});
    this.entityForm.patchValue({description: entity.description});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        if (!this.isAdd) {
          this.entityForm.get('isolatedTbCore').disable({emitEvent: false});
          this.entityForm.get('isolatedTbRuleEngine').disable({emitEvent: false});
        }
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
