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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Tenant, TenantInfo } from '@app/shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { ContactBasedComponent } from '../../components/entity/contact-based.component';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-tenant',
  templateUrl: './tenant.component.html',
  styleUrls: ['./tenant.component.scss']
})
export class TenantComponent extends ContactBasedComponent<TenantInfo> {

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: TenantInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantInfo>,
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

  buildEntityForm(entity: TenantInfo): FormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required]],
        tenantProfileId: [entity ? entity.tenantProfileId : null, [Validators.required]],
        // isolatedTbCore: [entity ? entity.isolatedTbCore : false, []],
        // isolatedTbRuleEngine: [entity ? entity.isolatedTbRuleEngine : false, []],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : '']
          }
        )
      }
    );
  }

  updateEntityForm(entity: Tenant) {
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({tenantProfileId: entity.tenantProfileId});
    // this.entityForm.patchValue({isolatedTbCore: entity.isolatedTbCore});
    // this.entityForm.patchValue({isolatedTbRuleEngine: entity.isolatedTbRuleEngine});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        /* if (!this.isAdd) {
          this.entityForm.get('isolatedTbCore').disable({emitEvent: false});
          this.entityForm.get('isolatedTbRuleEngine').disable({emitEvent: false});
        } */
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
