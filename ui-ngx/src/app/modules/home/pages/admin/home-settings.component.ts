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

import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { DashboardService } from '@core/http/dashboard.service';
import { HomeDashboardInfo } from '@shared/models/dashboard.models';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';

@Component({
  selector: 'tb-home-settings',
  templateUrl: './home-settings.component.html',
  styleUrls: ['./home-settings.component.scss', './settings-card.scss']
})
export class HomeSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  homeSettings: FormGroup;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private dashboardService: DashboardService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.homeSettings = this.fb.group({
      dashboardId: [null],
      hideDashboardToolbar: [true]
    });
    this.dashboardService.getTenantHomeDashboardInfo().subscribe(
      (homeDashboardInfo) => {
        this.setHomeDashboardInfo(homeDashboardInfo);
      }
    );
  }

  save(): void {
    const strDashboardId = this.homeSettings.get('dashboardId').value;
    const dashboardId: DashboardId = strDashboardId ? new DashboardId(strDashboardId) : null;
    const hideDashboardToolbar = this.homeSettings.get('hideDashboardToolbar').value;
    const homeDashboardInfo: HomeDashboardInfo = {
      dashboardId,
      hideDashboardToolbar
    };
    this.dashboardService.setTenantHomeDashboardInfo(homeDashboardInfo).subscribe(
      () => {
        this.setHomeDashboardInfo(homeDashboardInfo);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.homeSettings;
  }

  private setHomeDashboardInfo(homeDashboardInfo: HomeDashboardInfo) {
    this.homeSettings.reset({
      dashboardId: homeDashboardInfo?.dashboardId?.id,
      hideDashboardToolbar: isDefinedAndNotNull(homeDashboardInfo?.hideDashboardToolbar) ?
        homeDashboardInfo?.hideDashboardToolbar : true
    });
  }

}
