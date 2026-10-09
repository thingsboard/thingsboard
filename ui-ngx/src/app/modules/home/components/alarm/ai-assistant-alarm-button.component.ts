// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { Operation, Resource } from '@shared/models/security.models';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { alarmRuleEntityTypeList } from '@shared/models/alarm-rule.models';

@Component({
  selector: 'tb-ai-assistant-alarm-button',
  templateUrl: './ai-assistant-alarm-button.component.html',
  standalone: false
})
export class AiAssistantAlarmButtonComponent implements OnInit {

  private authUser: AuthUser = getCurrentAuthUser(this.store);
  private hasPermission: boolean;

  constructor(private store: Store<AppState>,
              private panelService: AiAssistantPanelService,
              private userPermissionsService: UserPermissionsService) {
  }

  ngOnInit(): void {
    this.hasPermission = getCurrentAuthState(this.store).aiEnabled &&
      this.userPermissionsService.hasGenericPermission(Resource.AI, Operation.ALL) &&
      alarmRuleEntityTypeList.some(entityType =>
        this.userPermissionsService.hasGenericPermissionByEntityGroupType(Operation.READ_CALCULATED_FIELD, entityType));
  }

  show(): boolean {
    return this.hasPermission && !this.panelService.open();
  }

  toggleAiAssistant($event: Event): void {
    $event?.stopPropagation();
    this.panelService.toggle();
  }
}
