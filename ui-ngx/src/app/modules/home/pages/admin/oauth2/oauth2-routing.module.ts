// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable, NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { Authority } from '@shared/models/authority.enum';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { OAuth2Service } from '@core/http/oauth2.service';
import { Observable } from 'rxjs';
import { ClientsTableConfigResolver } from './clients/clients-table-config.resolver';
import { DomainTableConfigResolver } from '@home/pages/admin/oauth2/domains/domain-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { MenuId } from '@core/services/menu.models';

@Injectable()
export class OAuth2LoginProcessingUrlResolver  {

  constructor(private oauth2Service: OAuth2Service) {
  }

  resolve(): Observable<string> {
    return this.oauth2Service.getLoginProcessingUrl();
  }
}

export const oAuth2Routes: Routes = [
  {
    path: 'oauth2',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        label: 'admin.oauth2.oauth2',
        icon: 'mdi:shield-account'
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: {
            SYS_ADMIN: '/security-settings/oauth2/domains',
            TENANT_ADMIN: '/security-settings/oauth2/clients'
          }
        }
      },
      {
        path: 'domains',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.oauth2.domains',
          breadcrumb: {
            menuId: MenuId.domains
          }
        },
        resolve: {
          entitiesTableConfig: DomainTableConfigResolver
        }
      },
      {
        path: 'clients',
        data: {
          title: 'admin.oauth2.clients',
          breadcrumb: {
            menuId:MenuId.clients
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'admin.oauth2.clients'
            },
            resolve: {
              entitiesTableConfig: ClientsTableConfigResolver
            }
          },
          {
            path: 'details',
            children: [
              {
                path: ':entityId',
                component: EntityDetailsPageComponent,
                data: {
                  breadcrumb: {
                    labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                    icon: 'public'
                  } as BreadCrumbConfig<EntityDetailsPageComponent>,
                  auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
                  title: 'admin.oauth2.clients',
                  hideTabs: true,
                  backNavigationCommands: ['../..']
                },
                resolve: {
                  entitiesTableConfig: ClientsTableConfigResolver
                }
              }
            ]
          }
        ]
      }
    ]
  }
];

@NgModule({
  providers: [
    OAuth2LoginProcessingUrlResolver,
    ClientsTableConfigResolver,
    DomainTableConfigResolver
  ],
  imports: [RouterModule.forChild(oAuth2Routes)],
  exports: [RouterModule]
})
export class Oauth2RoutingModule {
}
