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
import {EntityComponent} from '@shared/components/entity/entity.component';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {TranslateService} from '@ngx-translate/core';
import {
  Dashboard,
  isPublicDashboard,
  getDashboardAssignedCustomersText,
  isCurrentPublicDashboardCustomer,
  DashboardInfo
} from '@shared/models/dashboard.models';
import {DashboardService} from '@core/http/dashboard.service';

@Component({
  selector: 'tb-dashboard-form',
  templateUrl: './dashboard-form.component.html',
  styleUrls: ['./dashboard-form.component.scss']
})
export class DashboardFormComponent extends EntityComponent<Dashboard | DashboardInfo> {

  dashboardScope: 'tenant' | 'customer' | 'customer_user';
  customerId: string;

  publicLink: string;
  assignedCustomersText: string;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              private dashboardService: DashboardService,
              public fb: FormBuilder) {
    super(store);
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
        title: [entity ? entity.title : '', [Validators.required]],
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
    this.entityForm.patchValue({configuration: {description: entity.configuration ? entity.configuration.description : ''}});
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
    this.assignedCustomersText = getDashboardAssignedCustomersText(entity);
    this.publicLink = this.dashboardService.getPublicDashboardLink(entity);
  }
}
