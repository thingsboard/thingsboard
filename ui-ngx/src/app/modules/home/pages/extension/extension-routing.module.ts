// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { ExtensionsTableConfigResolver } from '@home/pages/extension/extensions-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

export const extensionsRoutes: Routes = [
  {
    path: 'extensions',
    data: {
      breadcrumb: {
        menuId: MenuId.extensions
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'extension.extensions'
        },
        resolve: {
          entitiesTableConfig: ExtensionsTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'extension'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'extension.extensions',
          hideTabs: true
        },
        resolve: {
          entitiesTableConfig: ExtensionsTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(extensionsRoutes)],
  exports: [RouterModule],
  providers: [
    ExtensionsTableConfigResolver
  ]
})
export class ExtensionRoutingModule { }
