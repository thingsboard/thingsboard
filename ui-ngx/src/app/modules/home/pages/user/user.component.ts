///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, Inject, Optional } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { User } from '@shared/models/user.model';
import { selectAuth } from '@core/auth/auth.selectors';
import { map } from 'rxjs/operators';
import { Authority } from '@shared/models/authority.enum';
import { isDefinedAndNotNull, isUndefined } from '@core/utils';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.scss']
})
export class UserComponent extends EntityComponent<User> {

  authority = Authority;

  loginAsUserEnabled$ = this.store.pipe(
    select(selectAuth),
    map((auth) => auth.userTokenAccessEnabled)
  );

  constructor(protected store: Store<AppState>,
              @Optional() @Inject('entity') protected entityValue: User,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<User>,
              public fb: FormBuilder) {
    super(store, fb, entityValue, entitiesTableConfigValue);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  isUserCredentialsEnabled(): boolean {
      return this.entity.additionalInfo.userCredentialsEnabled === true;
  }

  isUserCredentialPresent(): boolean {
    return !(!this.entity || !this.entity.additionalInfo || isUndefined(this.entity.additionalInfo.userCredentialsEnabled));

  }

  buildForm(entity: User): FormGroup {
    return this.fb.group(
      {
        email: [entity ? entity.email : '', [Validators.required, Validators.email]],
        firstName: [entity ? entity.firstName : ''],
        lastName: [entity ? entity.lastName : ''],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            defaultDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null],
            defaultDashboardFullscreen: [entity && entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        )
      }
    );
  }

  updateForm(entity: User) {
    this.entityForm.patchValue({email: entity.email});
    this.entityForm.patchValue({firstName: entity.firstName});
    this.entityForm.patchValue({lastName: entity.lastName});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardId: entity.additionalInfo ? entity.additionalInfo.defaultDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {defaultDashboardFullscreen: entity.additionalInfo ? entity.additionalInfo.defaultDashboardFullscreen : false}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardHideToolbar: entity.additionalInfo &&
          isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true}});
  }

}
