// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { MenuId } from '@core/services/menu.models';
import { AiAssistantComponent } from '@home/pages/ai-assistant/ai-assistant.component';

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
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class AiAssistantRoutingModule { }
