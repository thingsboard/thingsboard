// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Route, RouterModule } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { NgModule } from '@angular/core';
import { integrationsRoute } from '@home/pages/integration/integration-routing.module';
import { convertersRoute } from '@home/pages/converter/converter-routing.module';
import { MenuId } from '@core/services/menu.models';

export const integrationsCenterRoute = (): Route => ({
  path: 'integrationsCenter',
  data: {
    auth: [Authority.TENANT_ADMIN],
    breadcrumb: {
      menuId: MenuId.integrations_center,
      skip: true
    }
  },
  children: [
    {
      path: '',
      children: [],
      data: {
        auth: [Authority.TENANT_ADMIN],
        redirectTo: 'integrations'
      }
    },
    integrationsRoute(),
    convertersRoute()
  ]
});

@NgModule({
  imports: [RouterModule.forChild([integrationsCenterRoute()])],
  exports: [RouterModule]
})
export class IntegrationsCenterRoutingModule { }
