// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, NgModule } from '@angular/core';
import { Router, RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { MenuId } from '@core/services/menu.models';
import { AiAssistantComponent } from '@home/pages/ai-assistant/ai-assistant.component';
import { NEW_SOLUTION_ID } from '@shared/models/solution-creator.models';

const routes: Routes = [
  {
    path: 'ai-assistant',
    component: AiAssistantComponent,
    data: {
      breadcrumb: {
        menuId: MenuId.ai_assistant
      },
      auth: [Authority.TENANT_ADMIN],
      canActivate: (userPermissionsService: UserPermissionsService): boolean => {
        return userPermissionsService.isAiEnabled() && userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL);
      },
    }
  },
  {
    path: 'ai-solution-creator',
    pathMatch: 'full',
    redirectTo: ({queryParams}) => inject(Router).createUrlTree(['/ai-assistant'], {
      queryParams: {solutionId: queryParams['solutionId'] || NEW_SOLUTION_ID}
    })
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class AiAssistantRoutingModule { }
