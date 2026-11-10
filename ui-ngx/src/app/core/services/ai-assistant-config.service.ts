// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AiAssistantPanelConfig, AiAssistantViewType } from '@shared/models/ai-chat.models';
import { EntityId } from '@shared/models/id/entity-id';

@Injectable({
  providedIn: 'root'
})
export class AiAssistantConfigService {

  private store = inject<Store<AppState>>(Store);
  private userPermissionsService = inject(UserPermissionsService);
  private translate = inject(TranslateService);

  tableConfig(listView: AiAssistantViewType, entityView?: AiAssistantViewType, listEntityId?: EntityId): AiAssistantPanelConfig | null {
    if (!getCurrentAuthState(this.store).aiEnabled || !this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL)) {
      return null;
    }
    return {
      view: {listView, entityView, listEntityId},
      initialPromptPlaceholder: this.translate.instant('ai-assistant.welcome.placeholder')
    };
  }
}

export const isEdgeScope = (scope: string): boolean => scope === 'edge' || scope === 'edges';
