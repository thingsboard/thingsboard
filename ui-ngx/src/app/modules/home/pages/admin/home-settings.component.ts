// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { DashboardService } from '@core/http/dashboard.service';
import { HomeDashboardInfo } from '@shared/models/dashboard.models';
import { isDefinedAndNotNull } from '@core/utils';
import { DashboardId } from '@shared/models/id/dashboard-id';

@Component({
    selector: 'tb-home-settings',
    templateUrl: './home-settings.component.html',
    styleUrls: ['./home-settings.component.scss', './settings-card.scss'],
    standalone: false
})
export class HomeSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  homeSettings: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private dashboardService: DashboardService,
              public fb: UntypedFormBuilder) {
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

  confirmForm(): UntypedFormGroup {
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
