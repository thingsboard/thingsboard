///
/// Copyright © 2016-2025 The Thingsboard Authors
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
      auth: [Authority.TENANT_ADMIN],
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
