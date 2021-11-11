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

import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import {
  Dashboard,
  getDashboardAssignedCustomersText,
  isCurrentPublicDashboardCustomer,
  isPublicDashboard
} from '@shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { isEqual } from '@core/utils';

@Component({
  selector: 'tb-dashboard-form',
  templateUrl: './dashboard-form.component.html',
  styleUrls: ['./dashboard-form.component.scss']
})
export class DashboardFormComponent extends EntityComponent<Dashboard> {

  dashboardScope: 'tenant' | 'customer' | 'customer_user' | 'edge';
  customerId: string;

  publicLink: string;
  assignedCustomersText: string;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dashboardService: DashboardService,
              @Inject('entity') protected entityValue: Dashboard,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Dashboard>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  ngOnInit() {
    this.dashboardScope = this.entitiesTableConfig.componentsData.dashboardScope;
    this.customerId = this.entitiesTableConfig.componentsData.customerId;
    super.ngOnInit();
  }

  isPublic(entity: Dashboard): boolean {
    return isPublicDashboard(entity);
  }

  isCurrentPublicCustomer(entity: Dashboard): boolean {
    return isCurrentPublicDashboardCustomer(entity, this.customerId);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: Dashboard): FormGroup {
    this.updateFields(entity);
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : null],
        mobileHide: [entity ? entity.mobileHide : false],
        mobileOrder: [entity ? entity.mobileOrder : null, [Validators.pattern(/^-?[0-9]+$/)]],
        configuration: this.fb.group(
          {
            description: [entity && entity.configuration ? entity.configuration.description : ''],
          }
        )
      }
    );
  }

  updateForm(entity: Dashboard) {
    this.updateFields(entity);
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({image: entity.image});
    this.entityForm.patchValue({mobileHide: entity.mobileHide});
    this.entityForm.patchValue({mobileOrder: entity.mobileOrder});
    this.entityForm.patchValue({configuration: {description: entity.configuration ? entity.configuration.description : ''}});
  }

  prepareFormValue(formValue: any): any {
    const preparedValue = super.prepareFormValue(formValue);
    preparedValue.configuration = {...(this.entity.configuration || {}), ...(preparedValue.configuration || {})};
    return preparedValue;
  }

  onPublicLinkCopied($event) {
    this.store.dispatch(new ActionNotificationShow(
     {
        message: this.translate.instant('dashboard.public-link-copied-message'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  private updateFields(entity: Dashboard): void {
    if (entity && !isEqual(entity, {})) {
      this.assignedCustomersText = getDashboardAssignedCustomersText(entity);
      this.publicLink = this.dashboardService.getPublicDashboardLink(entity);
    }
  }
}
