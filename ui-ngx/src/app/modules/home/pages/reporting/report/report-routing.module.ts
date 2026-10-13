// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { Route } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { ReportsTableConfigResolver } from '@home/pages/reporting/report/reports-table-config.resolver';
import { MenuId } from '@core/services/menu.models';

export const reportsRoute: Route = {
  path: 'reports',
  data: {
    breadcrumb: {
      menuId: MenuId.reports
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'report.reports'
      },
      resolve: {
        entitiesTableConfig: ReportsTableConfigResolver
      }
    }
  ]
}

// @dynamic
@NgModule({
  providers: [
    ReportsTableConfigResolver
  ]
})
export class ReportRoutingModule { }
