///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';

import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { GeneralSettingsComponent } from '@modules/home/pages/admin/general-settings.component';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { forkJoin, of } from 'rxjs';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { HomeSettingsComponent } from '@home/pages/admin/home-settings.component';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { ResourcesLibraryTableConfigResolver } from '@home/pages/admin/resource/resources-library-table-config.resolve';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { QueuesTableConfigResolver } from '@home/pages/admin/queue/queues-table-config.resolver';
import { RepositoryAdminSettingsComponent } from '@home/pages/admin/repository-admin-settings.component';
import { AutoCommitAdminSettingsComponent } from '@home/pages/admin/auto-commit-admin-settings.component';
import { TwoFactorAuthSettingsComponent } from '@home/pages/admin/two-factor-auth-settings.component';
import { widgetsLibraryRoutes } from '@home/pages/widget/widget-library-routing.module';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { auditLogsRoutes } from '@home/pages/audit-log/audit-log-routing.module';
import { ImageGalleryComponent } from '@shared/components/image/image-gallery.component';
import { oAuth2Routes } from '@home/pages/admin/oauth2/oauth2-routing.module';
import { ImageResourceType, IMAGES_URL_PREFIX, ResourceSubType } from '@shared/models/resource.models';
import { ScadaSymbolComponent } from '@home/pages/scada-symbol/scada-symbol.component';
import { ImageService } from '@core/http/image.service';
import { ScadaSymbolData } from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { MenuId } from '@core/services/menu.models';
import { catchError } from 'rxjs/operators';
import { JsLibraryTableConfigResolver } from '@home/pages/admin/resource/js-library-table-config.resolver';
import { TrendzSettingsComponent } from '@home/pages/admin/trendz-settings.component';
import { aiModelRoutes } from '@home/pages/ai-model/ai-model-routing.module';

export const scadaSymbolResolver: ResolveFn<ScadaSymbolData> =
  (route: ActivatedRouteSnapshot,
   state: RouterStateSnapshot,
   router = inject(Router),
   imageService = inject(ImageService)) => {
    const type: ImageResourceType = route.params.type;
    const key = decodeURIComponent(route.params.key);
    return forkJoin({
      imageResource: imageService.getImageInfo(type, key),
      scadaSymbolContent: imageService.getImageString(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`)
    }).pipe(
      catchError(() => {
        router.navigate(['/resources/scada-symbols']);
        return of(null);
      })
    );
};

export const scadaSymbolBreadcumbLabelFunction: BreadCrumbLabelFunction<ScadaSymbolComponent>
  = ((route, translate, component) =>
  component.symbolData?.imageResource?.title);

const routes: Routes = [
  {
    path: 'resources',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.resources
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: '/resources/widgets-library'
        }
      },
      ...widgetsLibraryRoutes,
      {
        path: 'images',
        data: {
          breadcrumb: {
            menuId: MenuId.images
          }
        },
        children: [
          {
            path: '',
            component: ImageGalleryComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'image.gallery',
              imageSubType: ResourceSubType.IMAGE
            }
          }
        ]
      },
      {
        path: 'scada-symbols',
        data: {
          breadcrumb: {
            menuId: MenuId.scada_symbols
          }
        },
        children: [
          {
            path: '',
            component: ImageGalleryComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'scada.symbols',
              imageSubType: ResourceSubType.SCADA_SYMBOL
            }
          },
          {
            path: ':type/:key',
            component: ScadaSymbolComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: scadaSymbolBreadcumbLabelFunction,
                icon: 'view_in_ar'
              } as BreadCrumbConfig<ScadaSymbolComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'scada.symbol.symbol'
            },
            resolve: {
              symbolData: scadaSymbolResolver
            }
          },
        ]
      },
      {
        path: 'resources-library',
        data: {
          breadcrumb: {
            menuId: MenuId.resources_library
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library',
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'mdi:rhombus-split'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'resource.resources-library'
            },
            resolve: {
              entitiesTableConfig: ResourcesLibraryTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'javascript-library',
        data: {
          breadcrumb: {
            menuId: MenuId.javascript_library
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'javascript.javascript-library',
            },
            resolve: {
              entitiesTableConfig: JsLibraryTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'mdi:language-javascript'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN],
              title: 'javascript.javascript-library'
            },
            resolve: {
              entitiesTableConfig: JsLibraryTableConfigResolver
            }
          }
        ]
      }
    ]
  },
  {
    path: 'settings',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      showMainLoadingBar: false,
      breadcrumb: {
        menuId: MenuId.settings
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: {
            SYS_ADMIN: '/settings/general',
            TENANT_ADMIN: '/settings/home'
          }
        }
      },
      {
        path: 'general',
        component: GeneralSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.general-settings',
          breadcrumb: {
            menuId: MenuId.general
          }
        }
      },
      {
        path: 'outgoing-mail',
        component: MailServerComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.outgoing-mail-settings',
          breadcrumb: {
            menuId: MenuId.mail_server
          }
        }
      },
      {
        path: 'notifications',
        component: SmsProviderComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.notifications-settings',
          breadcrumb: {
            menuId: MenuId.notification_settings
          }
        }
      },
      {
        path: 'queues',
        data: {
          breadcrumb: {
            menuId: MenuId.queues
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.SYS_ADMIN],
              title: 'admin.queues'
            },
            resolve: {
              entitiesTableConfig: QueuesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'swap_calls'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN],
              title: 'admin.queues'
            },
            resolve: {
              entitiesTableConfig: QueuesTableConfigResolver
            }
          }
        ]
      },
      {
        path: 'home',
        component: HomeSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.home-settings',
          breadcrumb: {
            menuId: MenuId.home_settings
          }
        }
      },
      {
        path: 'repository',
        component: RepositoryAdminSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.repository-settings',
          breadcrumb: {
            menuId: MenuId.repository_settings
          }
        }
      },
      {
        path: 'auto-commit',
        component: AutoCommitAdminSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.auto-commit-settings',
          breadcrumb: {
            menuId: MenuId.auto_commit_settings
          }
        }
      },
      {
        path: 'trendz',
        component: TrendzSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'admin.trendz-settings',
          breadcrumb: {
            menuId: MenuId.trendz_settings
          }
        }
      },
      ...aiModelRoutes,
      {
        path: 'security-settings',
        redirectTo: '/security-settings/general'
      },
      {
        path: 'oauth2',
        redirectTo: '/security-settings/oauth2'
      },
      {
        path: 'resources-library',
        pathMatch: 'full',
        redirectTo: '/resources/resources-library'
      },
      {
        path: 'resources-library/:entityId',
        redirectTo: '/resources/resources-library/:entityId'
      },
      {
        path: '2fa',
        redirectTo: '/security-settings/2fa'
      },
      {
        path: 'sms-provider',
        redirectTo: '/settings/notifications'
      }
    ]
  },
  {
    path: 'security-settings',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.security_settings
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: {
            SYS_ADMIN: '/security-settings/general',
            TENANT_ADMIN: '/security-settings/auditLogs'
          }
        }
      },
      {
        path: 'general',
        component: SecuritySettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.general',
          breadcrumb: {
            menuId: MenuId.security_settings_general
          }
        }
      },
      {
        path: '2fa',
        component: TwoFactorAuthSettingsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN],
          title: 'admin.2fa.2fa',
          breadcrumb: {
            menuId: MenuId.two_fa
          }
        }
      },
      ...oAuth2Routes,
      ...auditLogsRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    ResourcesLibraryTableConfigResolver,
    JsLibraryTableConfigResolver,
    QueuesTableConfigResolver
  ]
})
export class AdminRoutingModule { }
