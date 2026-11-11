// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { Route } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import {
  ScheduledReportsTableConfigResolver
} from '@home/pages/reporting/scheduling/scheduled-reports-table-config.resolver';
import { MenuId } from '@core/services/menu.models';

export const scheduledReportsRoute: Route = {
  path: 'scheduling',
  data: {
    breadcrumb: {
      menuId: MenuId.report_scheduling
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
        title: 'scheduled-report.scheduled-reports'
      },
      resolve: {
        entitiesTableConfig: ScheduledReportsTableConfigResolver
      }
    }
  ]
}

// @dynamic
@NgModule({
  providers: [
    ScheduledReportsTableConfigResolver
  ]
})
export class ScheduledReportRoutingModule { }
