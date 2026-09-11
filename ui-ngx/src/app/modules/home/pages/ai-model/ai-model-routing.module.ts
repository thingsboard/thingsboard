// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { RouterModule, Routes } from '@angular/router';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { MenuId } from '@core/services/menu.models';
import { AiModelsTableConfigResolver } from '@home/pages/ai-model/ai-model-table-config.resolve';
import { NgModule } from '@angular/core';

export const aiModelRoutes: Routes = [
  {
    path: 'ai-models',
    component: EntitiesTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'ai-models.ai-models',
      breadcrumb: {
        menuId: MenuId.ai_models
      }
    },
    resolve: {
      entitiesTableConfig: AiModelsTableConfigResolver
    }
  }
];

@NgModule({
  providers: [
    AiModelsTableConfigResolver
  ],
  imports: [RouterModule.forChild(aiModelRoutes)],
  exports: [RouterModule],
})
export class AiModelRoutingModule { }
