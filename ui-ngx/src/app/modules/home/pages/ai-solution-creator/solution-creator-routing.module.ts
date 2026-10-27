// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { MenuId } from '@core/services/menu.models';
import { SolutionCreatorComponent } from '@home/pages/ai-solution-creator/solution-creator.component';

const routes: Routes = [
  {
    path: 'ai-solution-creator',
    component: SolutionCreatorComponent,
    data: {
      breadcrumb: {
        menuId: MenuId.ai_solution_creator
      },
      auth: [Authority.TENANT_ADMIN],
      canActivate: (userPermissionsService: UserPermissionsService): boolean => {
        return userPermissionsService.isAiEnabled() && userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL);
      },
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class SolutionCreatorRoutingModule { }
