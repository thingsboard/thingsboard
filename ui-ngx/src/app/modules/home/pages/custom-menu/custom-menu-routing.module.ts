// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ActivatedRouteSnapshot, ResolveFn, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { inject, NgModule } from '@angular/core';
import { MenuId } from '@core/services/menu.models';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import {
  afterLoadCustomMenuConfig,
  CustomMenuConfig,
  CustomMenuInfo
} from '@shared/models/custom-menu.models';
import { CustomMenuService } from '@core/http/custom-menu.service';
import { CustomMenuConfigComponent } from '@home/pages/custom-menu/custom-menu-config.component';
import { map } from 'rxjs/operators';
import { CustomMenuTableComponent } from '@home/pages/custom-menu/custom-menu-table.component';

const customMenuConfigBreadcrumbLabelFunction: BreadCrumbLabelFunction<any> = ((route) =>
  route.data.customMenu.name);

const customMenuResolver: ResolveFn<CustomMenuInfo> = (route: ActivatedRouteSnapshot) => {
  const customMenuId = route.params.customMenuId;
  return inject(CustomMenuService).getCustomMenuInfo(customMenuId);
};

const customMenuConfigResolver: ResolveFn<CustomMenuConfig> = (route: ActivatedRouteSnapshot,
                                                               _state: RouterStateSnapshot,
                                                               customMenuService = inject(CustomMenuService)) => {
  const customMenuId = route.params.customMenuId;
  return customMenuService.getCustomMenuConfig(customMenuId).pipe(
    map((config) => {
      const customMenu: CustomMenuInfo = route.parent.data.customMenu;
      const scope = customMenu.scope;
      return afterLoadCustomMenuConfig(config, scope);
    })
  );
};

export const CustomMenuRoutes: Routes = [
  {
    path: 'customMenu',
    data: {
      breadcrumb: {
        menuId: MenuId.custom_menu
      }
    },
    children: [
      {
        path: '',
        component: CustomMenuTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'custom-menu.custom-menu'
        }
      },
      {
        path: ':customMenuId',
        data: {
          breadcrumb: {
            labelFunction: customMenuConfigBreadcrumbLabelFunction,
            icon: 'list'
          } as BreadCrumbConfig<any>,
        },
        resolve: {
          customMenu: customMenuResolver
        },
        children: [
          {
            path: '',
            component: CustomMenuConfigComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'custom-menu.custom-menu-config'
            },
            resolve: {
              customMenuConfig: customMenuConfigResolver
            }
          }
        ]
      }
    ]
  }
];

@NgModule({})
export class CustomMenuRoutingModule { }
