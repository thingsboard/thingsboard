// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { AuditLogTableComponent } from '@home/components/audit-log/audit-log-table.component';
import { MenuId } from '@core/services/menu.models';

export const auditLogsRoutes: Routes = [
  {
    path: 'auditLogs',
    component: AuditLogTableComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'audit-log.audit-logs',
      breadcrumb: {
        menuId: MenuId.audit_log
      },
      isPage: true
    }
  }
];

const routes: Routes = [
  {
    path: 'auditLogs',
    redirectTo: '/security-settings/auditLogs'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class AuditLogRoutingModule { }
