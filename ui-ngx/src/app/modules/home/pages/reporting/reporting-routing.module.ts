// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Route, RouterModule } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { reportTemplatesRoute } from '@home/pages/reporting/template/report-template-routing.module';
import { MenuId } from '@core/services/menu.models';
import { scheduledReportsRoute } from '@home/pages/reporting/scheduling/scheduled-report-routing.module';
import { reportsRoute } from '@home/pages/reporting/report/report-routing.module';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestPePackComponent } from '@home/components/pe-pack/request-pe-pack.component';
import { PlatformFeature } from '@shared/models/subscription.models';

const disabledReportingReplaceComponentFunction = (store: Store<AppState>) => {
  const authState = getCurrentAuthState(store);
  if (!authState.reportingEnabled) {
    return RequestPePackComponent;
  } else {
    return null;
  }
}

export const reportingRoute: Route = {
  path: 'reporting',
  component: RouterTabsComponent,
  data: {
    auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
    platformFeature: PlatformFeature.REPORTING,
    replaceComponent: disabledReportingReplaceComponentFunction,
    breadcrumb: {
      menuId: MenuId.reporting
    }
  },
  children: [
    {
      path: '',
      children: [],
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        redirectTo: 'templates'
      }
    },
    reportTemplatesRoute,
    scheduledReportsRoute,
    reportsRoute
  ]
};

@NgModule({
  imports: [RouterModule.forChild([reportingRoute])],
  exports: [RouterModule]
})
export class ReportingRoutingModule { }
