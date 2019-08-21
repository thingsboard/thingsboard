///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityComponent} from '../../components/entity/entity.component';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {EntityType} from '@shared/models/entity-type.models';
import {NULL_UUID} from '@shared/models/id/has-uuid';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {TranslateService} from '@ngx-translate/core';
import {AssetInfo} from '@app/shared/models/asset.models';
import {RuleChain} from '@shared/models/rule-chain.models';

@Component({
  selector: 'tb-rulechain',
  templateUrl: './rulechain.component.html',
  styleUrls: ['./rulechain.component.scss']
})
export class RuleChainComponent extends EntityComponent<RuleChain> {

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: RuleChain): FormGroup {
    return this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required]],
        debugMode: [entity ? entity.debugMode : false],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: RuleChain) {
    this.entityForm.patchValue({name: entity.name});
    this.entityForm.patchValue({debugMode: entity.debugMode});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
  }


  onRuleChainIdCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('rulechain.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }
}
