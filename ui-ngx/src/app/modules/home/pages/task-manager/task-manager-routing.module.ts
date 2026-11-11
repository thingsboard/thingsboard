// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TaskManagerTableConfigResolver } from '@home/pages/task-manager/task-manager-table-config.resolver';
import { NgModule } from '@angular/core';

export const taskManagerRoutes: Routes = [
  {
    path: 'taskManager',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'task.task-manager',
      breadcrumb: {
        menuId: MenuId.task_manager
      }
    },
    resolve: {
      entitiesTableConfig: TaskManagerTableConfigResolver
    }
  }
];

@NgModule({
  imports: [],
  exports: [],
  providers: [
    TaskManagerTableConfigResolver
  ]
})
export class TaskManagerRoutingModule { }
