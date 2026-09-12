// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UsersTableConfigResolver } from '@modules/home/pages/user/users-table-config.resolver';
import { Authority } from '@shared/models/authority.enum';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';

const routes: Routes = [
  {
    path: 'users',
    data: {
      breadcrumb: {
        skip: true
      }
    },
    children: [
      {
        path: '',
        redirectTo: '/',
        pathMatch: 'full'
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'account_circle'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'user.user',
        },
        resolve: {
          entitiesTableConfig: UsersTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    UsersTableConfigResolver
  ]
})
export class UserRoutingModule { }
