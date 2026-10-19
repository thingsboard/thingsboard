// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { Route, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { ConvertersTableConfigResolver } from './converters-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { RequestPePackComponent } from '@home/components/pe-pack/request-pe-pack.component';
import { PlatformFeature } from '@shared/models/subscription.models';

const disabledConverterReplaceComponentFunction = (store: Store<AppState>) => {
  const authState = getCurrentAuthState(store);
  if (!authState.integrationsEnabled) {
    return RequestPePackComponent;
  } else {
    return null;
  }
}

export const convertersRoute = (): Route => (
{
  path: 'converters',
  data: {
    breadcrumb: {
      menuId: MenuId.converters
    }
  },
  children: [
    {
      path: '',
      component: EntitiesTableComponent,
      data: {
        auth: [Authority.TENANT_ADMIN],
        title: 'converter.converters',
        platformFeature: PlatformFeature.INTEGRATIONS,
        replaceComponent: disabledConverterReplaceComponentFunction,
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    },
    {
      path: ':entityId',
      component: EntityDetailsPageComponent,
      canDeactivate: [ConfirmOnExitGuard],
      data: {
        breadcrumb: {
          labelFunction: entityDetailsPageBreadcrumbLabelFunction,
          icon: 'transform'
        } as BreadCrumbConfig<EntityDetailsPageComponent>,
        auth: [Authority.TENANT_ADMIN],
        title: 'converter.converters'
      },
      resolve: {
        entitiesTableConfig: ConvertersTableConfigResolver
      }
    }
  ]
});

const routes: Routes = [
  {
    path: 'converters',
    pathMatch: 'full',
    redirectTo: '/integrationsCenter/converters'
  },
  {
    path: 'converters/:entityId',
    redirectTo: '/integrationsCenter/converters/:entityId'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    ConvertersTableConfigResolver
  ]
})
export class ConverterRoutingModule { }
