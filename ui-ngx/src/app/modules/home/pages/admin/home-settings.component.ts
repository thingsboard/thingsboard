// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { AuthState } from '@core/auth/auth.models';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Observable } from 'rxjs/internal/Observable';
import { Authority } from '@shared/models/authority.enum';

@Component({
    selector: 'tb-home-settings',
    templateUrl: './home-settings.component.html',
    styleUrls: ['./home-settings.component.scss', './settings-card.scss'],
    standalone: false
})
export class HomeSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  authState: AuthState = getCurrentAuthState(this.store);

  authUser: AuthUser = this.authState.authUser;

  readonly = !this.userPermissionsService.hasGenericPermission(Resource.WHITE_LABELING, Operation.WRITE);

  homeSettings: UntypedFormGroup;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private dashboardService: DashboardService,
              public fb: UntypedFormBuilder,
              private userPermissionsService: UserPermissionsService) {
    super(store);
  }

  ngOnInit() {
    this.homeSettings = this.fb.group({
      dashboardId: [null],
      hideDashboardToolbar: [true]
    });
    if (this.readonly) {
      this.homeSettings.disable({emitEvent: false});
    }

    let homeDashboardInfoObservable: Observable<HomeDashboardInfo>;

    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      homeDashboardInfoObservable = this.dashboardService.getTenantHomeDashboardInfo();
    } else if (this.authUser.authority === Authority.CUSTOMER_USER) {
      homeDashboardInfoObservable = this.dashboardService.getCustomerHomeDashboardInfo();
    }
    if (homeDashboardInfoObservable) {
      homeDashboardInfoObservable.subscribe(
        (homeDashboardInfo) => {
          this.setHomeDashboardInfo(homeDashboardInfo);
        }
      );
    }
  }

  save(): void {
    const strDashboardId = this.homeSettings.get('dashboardId').value;
    const dashboardId: DashboardId = strDashboardId ? new DashboardId(strDashboardId) : null;
    const hideDashboardToolbar = this.homeSettings.get('hideDashboardToolbar').value;
    const homeDashboardInfo: HomeDashboardInfo = {
      dashboardId,
      hideDashboardToolbar
    };
    let setHomeDashboardInfoObservable: Observable<any>;
    if (this.authUser.authority === Authority.TENANT_ADMIN) {
      setHomeDashboardInfoObservable = this.dashboardService.setTenantHomeDashboardInfo(homeDashboardInfo);
    } else if (this.authUser.authority === Authority.CUSTOMER_USER) {
      setHomeDashboardInfoObservable = this.dashboardService.setCustomerHomeDashboardInfo(homeDashboardInfo);
    }
    if (setHomeDashboardInfoObservable) {
      setHomeDashboardInfoObservable.subscribe(
        () => {
          this.setHomeDashboardInfo(homeDashboardInfo);
        }
      );
    }
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
