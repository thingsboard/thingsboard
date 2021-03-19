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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';

import { Authority } from '@shared/models/authority.enum';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { Dashboard } from '@app/shared/models/dashboard.models';
import { DashboardService } from '@core/http/dashboard.service';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { DashboardResolver } from '@app/modules/home/pages/dashboard/dashboard-routing.module';
import { UtilsService } from '@core/services/utils.service';
import { Widget } from '@app/shared/models/widget.models';
import { MODULES_MAP } from '../../shared/models/constants';
import { modulesMap } from '../common/modules-map';

@Injectable()
export class WidgetEditorDashboardResolver implements Resolve<Dashboard> {

  constructor(private dashboardService: DashboardService,
              private dashboardUtils: DashboardUtilsService,
              private utils: UtilsService) {
  }

  resolve(route: ActivatedRouteSnapshot): Dashboard {
    const editWidgetInfo = this.utils.editWidgetInfo;
    const widget: Widget = {
      isSystemType: true,
      bundleAlias: 'customWidgetBundle',
      typeAlias: 'customWidget',
      type: editWidgetInfo.type,
      title: 'My widget',
      image: null,
      description: null,
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
    DashboardResolver,
    {
      provide: MODULES_MAP,
      useValue: modulesMap
    }
  ]
})
export class DashboardPagesRoutingModule { }
