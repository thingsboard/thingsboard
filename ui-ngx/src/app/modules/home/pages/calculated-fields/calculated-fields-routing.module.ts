///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { DestroyRef, inject, NgModule, Renderer2 } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Router, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { CalculatedFieldsTableComponent } from '@home/components/calculated-fields/calculated-fields-table.component';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { CalculatedFieldsTableConfig } from '@home/components/calculated-fields/calculated-fields-table-config';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { TranslateService } from '@ngx-translate/core';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatePipe } from '@angular/common';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { UtilsService } from '@core/services/utils.service';

export const CalculatedFieldsTableConfigResolver: ResolveFn<CalculatedFieldsTableConfig> =
  (_route: ActivatedRouteSnapshot,
   _state: RouterStateSnapshot,
   calculatedFieldsService = inject(CalculatedFieldsService),
   translate = inject(TranslateService),
   dialog = inject(MatDialog),
   store = inject(Store<AppState>),
   datePipe = inject(DatePipe),
   destroyRef = inject(DestroyRef),
   importExportService = inject(ImportExportService),
   entityDebugSettingsService = inject(EntityDebugSettingsService),
   utilsService = inject(UtilsService),
   router = inject(Router),
  ) => {
    return new CalculatedFieldsTableConfig(
      calculatedFieldsService,
      translate,
      dialog,
      datePipe,
      null,
      store,
      destroyRef,
      null,
      null,
      null,
      importExportService,
      entityDebugSettingsService,
      utilsService,
      router,
      false,
    );
  };

const routes: Routes = [
  {
    path: 'calculatedFields',
    data: {
      breadcrumb: {
        menuId: MenuId.calculated_fields
      }
    },
    children: [
      {
        path: '',
        component: CalculatedFieldsTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'entity.type-calculated-fields',
          isPage: true,
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'mdi:function-variant'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'entity.type-calculated-fields',
        },
        resolve: {
          entitiesTableConfig: CalculatedFieldsTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class CalculatedFieldsRoutingModule { }