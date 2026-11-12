// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, computed, HostListener, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AiChatsService } from '@core/services/ai-chats.service';
import { ChatLevel } from '@shared/models/ai-chat.models';
import { MatDialog } from '@angular/material/dialog';
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
      @let status = cue();
      <button mat-icon-button type="button" class="tb-ai-assistant-button" [attr.data-cue]="status?.level"
              tb-popover [tbAiTip]="buttonTip"
              [attr.aria-label]="status ? ('ai-assistant.ai-assistant-cue' | translate: {cue: status.tip}) : ('ai-assistant.ai-assistant' | translate)"
              (click)="toggle($event)">
        <mat-icon fontSet="material-icons-round">auto_awesome</mat-icon>
        @if (status && status.level !== 'running') {
          <span class="tb-ai-assistant-cue tb-ai-status-mark" [class]="status.level" aria-hidden="true"></span>
        }
      </button>
      <ng-template #buttonTip>
        <div class="tb-ai-tip-body tip-mini">
          <div class="tip-keyrow">
            <span>{{ status ? ('ai-assistant.ai-assistant-cue' | translate: {cue: status.tip}) : ('ai-assistant.ai-assistant' | translate) }}</span>
            <span class="tip-caps"><kbd>/</kbd></span>
          </div>
        </div>
      </ng-template>
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

  private dialog = inject(MatDialog);

  private chats = inject(AiChatsService);

  private translate = inject(TranslateService);

  readonly cue = computed<{level: ChatLevel; tip: string} | null>(() => {
    const chats = this.chats.statusChats();
    const titleOf = (title: string) => title || this.translate.instant('ai-assistant.new-chat');
    const attention = chats.filter(chat => this.chats.needsAttention(chat));
    if (attention.length) {
      const level: ChatLevel = attention.some(chat => this.chats.levelOf(chat) === 'needs') ? 'needs' : 'error';
      return {
        level,
        tip: attention.length === 1
          ? this.translate.instant('ai-assistant.waiting-one-tip', {title: titleOf(attention[0].title), status: this.chats.tipOf(attention[0])})
          : this.translate.instant('ai-assistant.waiting-many', {count: attention.length})
      };
    }
    const running = chats.filter(chat => this.chats.levelOf(chat) === 'running');
    if (running.length) {
      return {
        level: 'running',
        tip: running.length === 1
          ? this.translate.instant('ai-assistant.running-one-tip', {title: titleOf(running[0].title)})
          : this.translate.instant('ai-assistant.running-many', {count: running.length})
      };
    }
    return null;
  });

  constructor() {
    if (this.available) {
      this.chats.watchStatus();
    }
  }

  @HostListener('document:keydown', ['$event'])
  onSlash(event: KeyboardEvent): void {
    if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented || !this.available
      || this.panelService.open() || this.panelService.suppressed() || this.dialog.openDialogs.length
      || (event.target as HTMLElement)?.closest?.('input, textarea, select, [contenteditable]')
      || Array.from(document.querySelectorAll<HTMLElement>('.tb-ai-composer textarea')).some(field => field.offsetParent && !field.closest('tb-ai-assistant-panel:not(.open)'))) {
      return;
    }
    event.preventDefault();
    this.panelService.openPanel();
  }

  toggle($event: Event): void {
    $event.stopPropagation();
    this.panelService.openPanel();
  }
}
