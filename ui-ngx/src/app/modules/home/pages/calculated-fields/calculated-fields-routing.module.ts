// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { DestroyRef, inject, NgModule } from '@angular/core';
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
import { IotHubActionsService } from '@home/components/iot-hub/iot-hub-actions.service';

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
   iotHubActions = inject(IotHubActionsService),
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
      iotHubActions,
      true,
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