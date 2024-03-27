///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { EntityComponent } from '@home/components/entity/entity.component';
import { AlarmRuleInfo } from '@shared/models/alarm-rule.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
  selector: 'tb-alarm-rule',
  templateUrl: './alarm-rule.component.html',
  styleUrls: ['./alarm-rule.component.scss']
})
export class AlarmRuleComponent extends EntityComponent<AlarmRuleInfo> {

  constructor(protected store: Store<AppState>,
              @Inject('entity') protected entityValue: AlarmRuleInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<AlarmRuleInfo>,
              protected fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: AlarmRuleInfo): FormGroup {
    return this.fb.group({});
  }

  updateForm(entity: AlarmRuleInfo) {
  }
}
