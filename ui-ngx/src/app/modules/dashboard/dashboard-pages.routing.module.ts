// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, RouterModule, Routes } from '@angular/router';

import { Authority } from '@shared/models/authority.enum';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { DashboardResolver } from '@app/modules/home/pages/dashboard/dashboard-routing.module';
import { UtilsService } from '@core/services/utils.service';
import { Widget } from '@app/shared/models/widget.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';

@Injectable()
export class WidgetEditorDashboardResolver  {

  constructor(private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private utils: UtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Dashboard {
    const editWidgetInfo = this.utils.editWidgetInfo;
    const widget: Widget = {
      typeFullFqn: 'system.customWidget',
      type: editWidgetInfo.type,
      sizeX: editWidgetInfo.sizeX * 2,
      sizeY: editWidgetInfo.sizeY * 2,
      row: 2,
      col: 4,
      config: JSON.parse(editWidgetInfo.defaultConfig)
    };
    widget.config.title = widget.config.title || editWidgetInfo.widgetName;
    return this.dashboardUtils.createSingleWidgetDashboard(widget);
  }
}

const routes: Routes = [
  {
    path: 'dashboard/:dashboardId',
    component: DashboardPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      breadcrumb: {
        skip: true
      },
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'dashboard.dashboard',
      widgetEditMode: false,
      singlePageMode: true
    },
    resolve: {
      dashboard: DashboardResolver
    }
  },
  {
    path: 'widget-editor',
    component: DashboardPageComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      breadcrumb: {
        skip: true
      },
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'widget.editor',
      widgetEditMode: true,
      singlePageMode: true
    },
    resolve: {
      dashboard: WidgetEditorDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    WidgetEditorDashboardResolver,
    DashboardResolver
  ]
})
export class DashboardPagesRoutingModule { }
