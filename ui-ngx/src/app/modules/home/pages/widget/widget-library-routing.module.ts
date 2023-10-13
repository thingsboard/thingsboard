///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Injectable, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { WidgetsBundlesTableConfigResolver } from '@modules/home/pages/widget/widgets-bundles-table-config.resolver';
import { BreadCrumbConfig, BreadCrumbLabelFunction } from '@shared/components/breadcrumb';
import { Observable } from 'rxjs';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';
import { WidgetEditorComponent } from '@home/pages/widget/widget-editor.component';
import { map } from 'rxjs/operators';
import { detailsToWidgetInfo, WidgetInfo } from '@home/models/widget-component.models';
import { widgetType, WidgetTypeDetails, WidgetTypeInfo } from '@app/shared/models/widget.models';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { RouterTabsComponent } from '@home/components/router-tabs.component';
import { WidgetTypesTableConfigResolver } from '@home/pages/widget/widget-types-table-config.resolver';
import { WidgetsBundleWidgetsComponent } from '@home/pages/widget/widgets-bundle-widgets.component';

export interface WidgetEditorData {
  widgetTypeDetails: WidgetTypeDetails;
  widget: WidgetInfo;
}

@Injectable()
export class WidgetsBundleResolver implements Resolve<WidgetsBundle> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetsBundle> {
    const widgetsBundleId = route.params.widgetsBundleId;
    return this.widgetsService.getWidgetsBundle(widgetsBundleId);
  }
}

@Injectable()
export class WidgetsBundleWidgetsResolver implements Resolve<Array<WidgetTypeInfo>> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<Array<WidgetTypeInfo>> {
    const widgetsBundleId = route.params.widgetsBundleId;
    return this.widgetsService.getBundleWidgetTypeInfosList(widgetsBundleId);
  }
}

@Injectable()
export class WidgetEditorDataResolver implements Resolve<WidgetEditorData> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetEditorData> {
    const widgetTypeId = route.params.widgetTypeId;
    if (!widgetTypeId || Object.keys(widgetType).includes(widgetTypeId)) {
      let widgetTypeParam = widgetTypeId as widgetType;
      if (!widgetTypeParam) {
        widgetTypeParam = widgetType.timeseries;
      }
      return this.widgetsService.getWidgetTemplate(widgetTypeParam).pipe(
        map((widget) => {
          widget.widgetName = null;
          return {
            widgetTypeDetails: null,
            widget
          };
        })
      );
    } else {
      return this.widgetsService.getWidgetTypeById(widgetTypeId).pipe(
        map((result) => ({
          widgetTypeDetails: result,
          widget: detailsToWidgetInfo(result)
        }))
      );
    }
  }
}

export const widgetsBundleWidgetsBreadcumbLabelFunction: BreadCrumbLabelFunction<any> = ((route, translate) =>
  route.data.widgetsBundle.title);

export const widgetEditorBreadcumbLabelFunction: BreadCrumbLabelFunction<WidgetEditorComponent> =
  ((route, translate, component) =>
    component?.widget?.widgetName ?
      (component.widget.widgetName + (component.widget.deprecated ? ` (${translate.instant('widget.deprecated')})` : '')) : '');

const widgetTypesRoutes: Routes = [
  {
    path: 'widget-types',
    data: {
      breadcrumb: {
        label: 'widget.widgets',
        icon: 'now_widgets'
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
          widgetEditorData: WidgetEditorDataResolver
        }
      }
    ]
  },
];

const widgetsBundlesRoutes: Routes = [
  {
    path: 'widgets-bundles',
    data: {
      breadcrumb: {
        label: 'widgets-bundle.widgets-bundles',
        icon: 'now_widgets'
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
        path: ':widgetsBundleId',
        component: WidgetsBundleWidgetsComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'widgets-bundle.widgets-bundle-widgets',
          breadcrumb: {
            labelFunction: widgetsBundleWidgetsBreadcumbLabelFunction,
            icon: 'now_widgets'
          } as BreadCrumbConfig<any>,
          hideTabs: true
        },
        resolve: {
          widgetsBundle: WidgetsBundleResolver,
          widgets: WidgetsBundleWidgetsResolver
        }
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
        label: 'widget.widget-library',
        icon: 'now_widgets'
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
    WidgetsBundlesTableConfigResolver,
    WidgetsBundleResolver,
    WidgetsBundleWidgetsResolver,
    WidgetEditorDataResolver
  ]
})
export class WidgetLibraryRoutingModule { }
