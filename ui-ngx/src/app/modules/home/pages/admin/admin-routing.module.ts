// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { inject, NgModule } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  ResolveFn,
  Router,
  RouterModule,
  RouterStateSnapshot,
  Routes,
  UrlTree
} from '@angular/router';
import { MailServerComponent } from '@modules/home/pages/admin/mail-server.component';
import { SmsProviderComponent } from '@home/pages/admin/sms-provider.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { GeneralSettingsComponent } from '@home/pages/admin/general-settings.component';
import { SecuritySettingsComponent } from '@modules/home/pages/admin/security-settings.component';
import { MailTemplatesComponent } from '@home/pages/admin/mail-templates.component';
import { forkJoin, mergeMap, Observable, of } from 'rxjs';
import { MailTemplatesSettings } from '@shared/models/settings.models';
import { WhiteLabelingComponent } from '@home/pages/admin/white-labeling.component';
import { SelfRegistrationComponent } from '@home/pages/admin/self-registration.component';
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
import { rolesRoutes } from '@home/pages/role/role-routing.module';
import { WhiteLabelingService } from '@core/http/white-labeling.service';
import { CustomTranslationRoutes } from '@home/pages/custom-translation/custom-translation-routing.module';
import { oAuth2Routes } from '@home/pages/admin/oauth2/oauth2-routing.module';
import { ImageResourceType, IMAGES_URL_PREFIX, ResourceSubType } from '@shared/models/resource.models';
import { ScadaSymbolComponent } from '@home/pages/scada-symbol/scada-symbol.component';
import { ImageService } from '@core/http/image.service';
import { ScadaSymbolData } from '@home/pages/scada-symbol/scada-symbol-editor.models';
import { MenuId } from '@core/services/menu.models';
import { CustomMenuRoutes } from '@home/pages/custom-menu/custom-menu-routing.module';
import { catchError, tap } from 'rxjs/operators';
import { JsLibraryTableConfigResolver } from '@home/pages/admin/resource/js-library-table-config.resolver';
import { secretsRoutes } from '@home/pages/secret-storage/secret-storage-routing.module';
import { aiModelRoutes } from '@home/pages/ai-model/ai-model-routing.module';
import { LicenseManagementComponent } from '@home/pages/admin/license-management.component';
import { SubscriptionInfo } from '@shared/models/subscription.models';
import { AdminService } from '@core/http/admin.service';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { AuthService } from '@core/auth/auth.service';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { ActionUpdateLicenseParams } from '@core/auth/auth.actions';

export const subscriptionInfoResolver: ResolveFn<SubscriptionInfo> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store = inject(Store<AppState>),
  adminService = inject(AdminService)
): Observable<SubscriptionInfo> => getCurrentAuthState(store).licenseVersion > 1 ? refreshAndGetLicenseInfo(adminService, store) : of(null);

const refreshAndGetLicenseInfo = (adminService: AdminService,
                                                                        store: Store<AppState>): Observable<SubscriptionInfo> => {
  return adminService.getSubscriptionInfo().pipe(
    mergeMap((info: SubscriptionInfo) => {
      if (!info.offline && !info.nonProduction) {
        return adminService.refreshLicense().pipe(
          tap((refreshed) => {
            store.dispatch(new ActionUpdateLicenseParams(
              {
                edgeEnabled: refreshed.edgeEnabled,
                trendzEnabled: refreshed.trendzEnabled,
                communityGrantLicense: refreshed.communityGrantLicense
              }
            ));
          }),
          catchError(() => of(info))
        );
      } else {
        return of(info);
      }
    })
  );
};

/**
 * The license management page describes a v2 subscription, so a v1 license has nothing to show there and the page
 * would dereference the null the resolver returns. The menu entry is already hidden on the same condition
 * (see MenuId.license_management); this keeps a typed-in URL from reaching the page.
 */
export const licenseManagementGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  store = inject(Store<AppState>),
  authService = inject(AuthService)
): boolean | UrlTree => {
  const authState = getCurrentAuthState(store);
  return authState.licenseVersion > 1 ? true : authService.defaultUrl(true, authState);
};

export const mailTemplateSettingsResolver: ResolveFn<MailTemplatesSettings> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  wl = inject(WhiteLabelingService)
): Observable<MailTemplatesSettings> => wl.getMailTemplates(true);

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
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.resources
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            SYS_ADMIN: '/resources/widgets-library',
            TENANT_ADMIN: '/resources/widgets-library',
            CUSTOMER_USER: '/resources/images'
          }
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
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN, Authority.CUSTOMER_USER],
              title: 'image.images',
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
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN, Authority.CUSTOMER_USER],
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
              auth: [Authority.TENANT_ADMIN, Authority.SYS_ADMIN, Authority.CUSTOMER_USER],
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
              title: 'resource.files',
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
              title: 'javascript.scripts',
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
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
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
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            SYS_ADMIN: '/settings/general',
            TENANT_ADMIN: '/settings/home',
            CUSTOMER_USER: '/settings/home'
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
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
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
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
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
      ...aiModelRoutes,
      {
        path: 'security-settings',
        redirectTo: '/security-settings/general'
      },
      {
        path: 'selfRegistration',
        redirectTo: '/security-settings/selfRegistration'
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
    path: 'license',
    component: LicenseManagementComponent,
    canActivate: [licenseManagementGuard],
    data: {
      auth: [Authority.SYS_ADMIN],
      title: 'subscription.license-management',
      breadcrumb: {
        menuId: MenuId.license_management
      }
    },
    resolve: {
      subscriptionInfo: subscriptionInfoResolver
    }
  },
  {
    path: 'security-settings',
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      breadcrumb: {
        menuId: MenuId.security_settings
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            SYS_ADMIN: '/security-settings/general',
            TENANT_ADMIN: '/security-settings/2fa',
            CUSTOMER_USER: '/security-settings/roles',
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
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.2fa.2fa',
          breadcrumb: {
            menuId: MenuId.two_fa
          }
        }
      },
      ...oAuth2Routes,
      ...rolesRoutes,
      ...secretsRoutes,
      {
        path: 'selfRegistration',
        component: SelfRegistrationComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'self-registration.self-registration',
          breadcrumb: {
            menuId: MenuId.self_registration
          }
        }
      },
      ...auditLogsRoutes
    ]
  },
  {
    path: 'white-labeling',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      showMainLoadingBar: false,
      breadcrumb: {
        menuId: MenuId.white_labeling
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          redirectTo: {
            SYS_ADMIN: '/white-labeling/whiteLabel',
            TENANT_ADMIN: '/white-labeling/whiteLabel',
            CUSTOMER_USER: '/white-labeling/whiteLabel'
          }
        }
      },
      {
        path: 'whiteLabel',
        component: WhiteLabelingComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'white-labeling.white-labeling',
          isLoginWl: false,
          breadcrumb: {
            menuId: MenuId.white_labeling_general
          }
        }
      },
      {
        path: 'loginWhiteLabel',
        component: WhiteLabelingComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'white-labeling.login-white-labeling',
          isLoginWl: true,
          breadcrumb: {
            menuId: MenuId.login_white_labeling
          }
        }
      },
      {
        path: 'mail-template',
        component: MailTemplatesComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'admin.mail-template-settings',
          breadcrumb: {
            menuId: MenuId.mail_templates
          }
        },
        resolve: {
          mailTemplatesSettings: mailTemplateSettingsResolver
        }
      },
      ...CustomTranslationRoutes,
      ...CustomMenuRoutes,
      {
        path: 'selfRegistration',
        redirectTo: '/security-settings/selfRegistration'
      }
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
