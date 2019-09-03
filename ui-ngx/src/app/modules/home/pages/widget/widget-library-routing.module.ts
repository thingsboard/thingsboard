///
/// Copyright © 2016-2019 The Thingsboard Authors
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

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {RuleChainsTableConfigResolver} from '@modules/home/pages/rulechain/rulechains-table-config.resolver';
import {WidgetsBundlesTableConfigResolver} from '@modules/home/pages/widget/widgets-bundles-table-config.resolver';
import { WidgetLibraryComponent } from '@home/pages/widget/widget-library.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { User } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { UserService } from '@core/http/user.service';
import { Observable } from 'rxjs';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { WidgetService } from '@core/http/widget.service';

@Injectable()
export class WidgetsBundleResolver implements Resolve<WidgetsBundle> {

  constructor(private widgetsService: WidgetService) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<WidgetsBundle> {
    const widgetsBundleId = route.params.widgetsBundleId;
    return this.widgetsService.getWidgetsBundle(widgetsBundleId);
  }
}

const routes: Routes = [
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
        path: ':widgetsBundleId/widgetTypes',
        component: WidgetLibraryComponent,
        data: {
          auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN],
          title: 'widget.widget-library',
          breadcrumb: {
            labelFunction: ((route, translate) => route.data.widgetsBundle.title),
            icon: 'now_widgets'
          } as BreadCrumbConfig
        },
        resolve: {
          widgetsBundle: WidgetsBundleResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    WidgetsBundlesTableConfigResolver,
    WidgetsBundleResolver
  ]
})
export class WidgetLibraryRoutingModule { }
