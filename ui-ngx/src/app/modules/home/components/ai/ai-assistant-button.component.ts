// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { UserPermissionsService } from '@core/http/user-permissions.service';
import { Operation, Resource } from '@shared/models/security.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';

@Component({
  selector: 'tb-ai-assistant-button',
  template: `
    @if (available && !panelService.open() && !panelService.suppressed()) {
      <button mat-icon-button type="button" class="tb-ai-assistant-button"
              [matTooltip]="'ai-assistant.ai-assistant' | translate" matTooltipPosition="below"
              [attr.aria-label]="'ai-assistant.ai-assistant' | translate"
              (click)="toggle($event)">
        <mat-icon fontSet="material-icons-round">auto_awesome</mat-icon>
      </button>
    }
  `,
  styles: [`
    :host {
      display: contents;
    }
  `],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistantButtonComponent {

  panelService = inject(AiAssistantPanelService);

  private readonly authState = getCurrentAuthState(inject(Store<AppState>));

  readonly available = this.authState.aiEnabled
    && this.authState.authUser?.authority === Authority.TENANT_ADMIN
    && inject(UserPermissionsService).hasGenericPermission(Resource.AI, Operation.ALL);

  toggle($event: Event): void {
    $event.stopPropagation();
    this.panelService.openPanel();
  }
}
