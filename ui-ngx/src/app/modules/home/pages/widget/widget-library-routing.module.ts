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
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { WidgetsBundlesTableConfigResolver } from '@modules/home/pages/widget/widgets-bundles-table-config.resolver';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { WidgetEditorComponent } from '@home/pages/widget/widget-editor.component';
import { map } from 'rxjs/operators';
import { detailsToWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';
import {
  migrateWidgetTypeToDynamicForms,
  widgetType,
  WidgetTypeDetails,
  WidgetTypeInfo
} from '@app/shared/models/widget.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { WidgetTypesTableConfigResolver } from '@home/pages/widget/widget-types-table-config.resolver';
import { WidgetsBundleWidgetsComponent } from '@home/pages/widget/widgets-bundle-widgets.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { MenuId } from '@core/services/menu.models';

export interface WidgetEditorData {
  widgetTypeDetails: WidgetTypeDetails;
  widget: WidgetInfo;
}

const widgetsBundleResolver: ResolveFn<WidgetsBundle> = (route: ActivatedRouteSnapshot) => {
  const widgetsBundleId = route.params.widgetsBundleId;
  return inject(WidgetService).getWidgetsBundle(widgetsBundleId);
};

const widgetsBundleWidgetsResolver: ResolveFn<Array<WidgetTypeInfo>> = (route: ActivatedRouteSnapshot) => {
  const widgetsBundleId = route.params.widgetsBundleId;
  return inject(WidgetService).getBundleWidgetTypeInfosList(widgetsBundleId);
};

const widgetEditorDataResolver: ResolveFn<WidgetEditorData> = (route: ActivatedRouteSnapshot) => {
  const widgetTypeId = route.params.widgetTypeId;
  if (!widgetTypeId || Object.keys(widgetType).includes(widgetTypeId)) {
    let widgetTypeParam = widgetTypeId as widgetType;
    if (!widgetTypeParam) {
      widgetTypeParam = widgetType.timeseries;
    }
    return inject(WidgetService).getWidgetTemplate(widgetTypeParam).pipe(
      map((widget) => {
        widget.widgetName = null;
        return {
          widgetTypeDetails: null,
          widget
        };
      })
    );
  } else {
    return inject(WidgetService).getWidgetTypeById(widgetTypeId).pipe(
      map((result) => {
        result = migrateWidgetTypeToDynamicForms(result);
        return {
          widgetTypeDetails: result,
          widget: detailsToWidgetInfo(result)
        };
      })
    );
  }
};

export const widgetsBundleWidgetsBreadcumbLabelFunction: BreadCrumbLabelFunction<any> = ((route) =>
  route.data.widgetsBundle.title);

export const widgetEditorBreadcumbLabelFunction: BreadCrumbLabelFunction<WidgetEditorComponent> =
  ((route, translate, component) =>
    component?.widget?.widgetName ?
      (component.widget.widgetName + (component.widget.deprecated ? ` (${translate.instant('widget.deprecated')})` : '')) : '');

const widgetEditorRouter: Routes = [
  {
    path: ':widgetTypeId',
    component: WidgetEditorComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      title: 'widget.editor',
      breadcrumb: {
        labelFunction: widgetEditorBreadcumbLabelFunction,
        icon: 'insert_chart'
      } as BreadCrumbConfig<WidgetEditorComponent>,
      hideTabs: true
    },
    resolve: {
      widgetEditorData: widgetEditorDataResolver
    }
  }
];

const widgetTypesRoutes: Routes = [
  {
    path: 'widget-types',
    data: {
      breadcrumb: {
        menuId: MenuId.widget_types
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'widget.widgets'
        },
        resolve: {
          entitiesTableConfig: WidgetTypesTableConfigResolver
        }
      },
      {
        path: 'details',
        children: [
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'now_widgets'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widget.widgets',
              hideTabs: true,
              backNavigationCommands: ['../..']
            },
            resolve: {
              entitiesTableConfig: WidgetTypesTableConfigResolver
            }
          }
        ]
      },
      ...widgetEditorRouter
    ]
  },
];

const widgetsBundlesRoutes: Routes = [
  {
    path: 'widgets-bundles',
    data: {
      breadcrumb: {
        menuId: MenuId.widgets_bundles
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'widgets-bundle.widgets-bundles'
        },
        resolve: {
          entitiesTableConfig: WidgetsBundlesTableConfigResolver
        }
      },
      {
        path: 'details',
        children: [
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'now_widgets'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widgets-bundle.widgets-bundles',
              hideTabs: true,
              backNavigationCommands: ['../..']
            },
            resolve: {
              entitiesTableConfig: WidgetsBundlesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':widgetsBundleId',
        data: {
          breadcrumb: {
            labelFunction: widgetsBundleWidgetsBreadcumbLabelFunction,
            icon: 'now_widgets'
          } as BreadCrumbConfig<any>,
        },
        resolve: {
          widgetsBundle: widgetsBundleResolver
        },
        children: [
          {
            path: '',
            component: WidgetsBundleWidgetsComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
              title: 'widgets-bundle.widgets-bundle-widgets',
              hideTabs: true
            },
            resolve: {
              widgets: widgetsBundleWidgetsResolver
            }
          },
          ...widgetEditorRouter
        ]
      }
    ]
  },
];

export const widgetsLibraryRoutes: Routes = [
  {
    path: 'widgets-library',
    component: RouterTabsComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
      breadcrumb: {
        menuId: MenuId.widget_library
      }
    },
    children: [
      {
        path: '',
        children: [],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          redirectTo: '/resources/widgets-library/widget-types'
        }
      },
      ...widgetTypesRoutes,
      ...widgetsBundlesRoutes
    ]
  }
];

const routes: Routes = [
  {
    path: 'widgets-bundles',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widgets-bundles'
  },
  {
    path: 'resources/widgets-bundles',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widgets-bundles'
  },
  {
    path: 'widgets-bundles/:widgetsBundleId/widgetTypes',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widgets-bundles/:widgetsBundleId'
  },
  {
    path: 'resources/widgets-bundles/:widgetsBundleId/widgetTypes',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widgets-bundles/:widgetsBundleId'
  },
  {
    path: 'widgets-bundles/:widgetsBundleId/widgetTypes/:widgetTypeId',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widget-types/:widgetTypeId'
  },
  {
    path: 'resources/widgets-bundles/:widgetsBundleId/widgetTypes/:widgetTypeId',
    pathMatch: 'full',
    redirectTo: '/resources/widgets-library/widget-types/:widgetTypeId'
  },
  {
    path: 'widgets-bundles/:widgetsBundleId/widgetTypes/add/:widgetType',
    redirectTo: '/resources/widgets-library/widget-types/:widgetType',
  },
  {
    path: 'resources/widgets-bundles/:widgetsBundleId/widgetTypes/add/:widgetType',
    redirectTo: '/resources/widgets-library/widget-types/:widgetType',
  }
];

// @dynamic
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    WidgetTypesTableConfigResolver,
    WidgetsBundlesTableConfigResolver
  ]
})
export class WidgetLibraryRoutingModule { }
