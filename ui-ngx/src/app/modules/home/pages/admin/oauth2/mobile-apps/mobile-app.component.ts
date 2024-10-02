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
import { MobileAppInfo } from '@shared/models/oauth2.models';
import { AppState } from '@core/core.state';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { TranslateService } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { isDefinedAndNotNull, randomAlphanumeric } from '@core/utils';
import { MatDialog } from '@angular/material/dialog';
import { ClientDialogComponent } from '@home/pages/admin/oauth2/clients/client-dialog.component';
import { EntityType } from '@shared/models/entity-type.models';

@Component({
  selector: 'tb-mobile-app',
  templateUrl: './mobile-app.component.html',
  styleUrls: []
})
export class MobileAppComponent extends EntityComponent<MobileAppInfo> {

  entityType = EntityType;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: MobileAppInfo,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<MobileAppInfo>,
              protected cd: ChangeDetectorRef,
              public fb: UntypedFormBuilder,
              private dialog: MatDialog) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  buildForm(entity: MobileAppInfo): UntypedFormGroup {
    return this.fb.group({
      pkgName: [entity?.pkgName ? entity.pkgName : '', [Validators.required, Validators.maxLength(255),
        Validators.pattern(/^\S+$/)]],
      appSecret: [entity?.appSecret ? entity.appSecret : btoa(randomAlphanumeric(64)),
        [Validators.required, this.base64Format]],
      oauth2Enabled: isDefinedAndNotNull(entity?.oauth2Enabled) ? entity.oauth2Enabled : true,
      oauth2ClientInfos: entity?.oauth2ClientInfos ? entity.oauth2ClientInfos.map(info => info.id.id) : []
    });
  }

  updateForm(entity: MobileAppInfo) {
    this.entityForm.patchValue({
      pkgName: entity.pkgName,
      appSecret: entity.appSecret,
      oauth2Enabled: entity.oauth2Enabled,
      oauth2ClientInfos: entity.oauth2ClientInfos?.map(info => info.id ? info.id.id : info)
    });
  }

  createClient($event: Event) {
    if ($event) {
      $event.stopPropagation();
      $event.preventDefault();
    }
    this.dialog.open<ClientDialogComponent>(ClientDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {}
    }).afterClosed()
      .subscribe((client) => {
        if (client) {
          const formValue = this.entityForm.get('oauth2ClientInfos').value ?
            [...this.entityForm.get('oauth2ClientInfos').value] : [];
          formValue.push(client.id.id);
          this.entityForm.get('oauth2ClientInfos').patchValue(formValue);
          this.entityForm.get('oauth2ClientInfos').markAsDirty();
        }
      });
  }

  private base64Format(control: UntypedFormControl): { [key: string]: boolean } | null {
    if (control.value === '') {
      return null;
    }
    try {
      const value = atob(control.value);
      if (value.length < 64) {
        return {minLength: true};
      }
      return null;
    } catch (e) {
      return {base64: true};
    }
  }

}
