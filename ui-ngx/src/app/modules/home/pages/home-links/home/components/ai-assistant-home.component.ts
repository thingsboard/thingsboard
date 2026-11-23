// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, HostListener, inject, signal, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { AiPromptMessage } from '@shared/models/ai-chat.models';
import { AiAssistantPanelService } from '@core/services/ai-assistant-panel.service';
import { AiPromptInputComponent } from '@home/components/ai/ai-prompt-input.component';

@Component({
  selector: 'tb-ai-assistant-home',
  template: `
    <div class="tb-ai-compose-row" [class.open]="composerFocused() || !!composerRef?.hasText()">
      <tb-ai-prompt-input #composerRef class="tb-ai-home-composer" [slim]="true"
                          (focusin)="composerFocused.set(true)" (focusout)="composerFocused.set(false)"
                          [placeholder]="'home.ai.placeholder' | translate"
                          (messageSent)="send($event)"></tb-ai-prompt-input>
      <a class="tb-ai-chats-btn" routerLink="/ai-assistant">
        <mat-icon aria-hidden="true">forum</mat-icon>
        <span class="tb-ai-chats-t">{{ 'home.ai.chats' | translate }}</span>
        <mat-icon class="tb-ai-chats-go" aria-hidden="true">chevron_right</mat-icon>
      </a>
    </div>
  `,
  styleUrls: ['./ai-assistant-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AiAssistantHomeComponent {

  @ViewChild(AiPromptInputComponent) composer: AiPromptInputComponent;

  readonly composerFocused = signal(false);

  private router = inject(Router);
  private panelService = inject(AiAssistantPanelService);
  private dialog = inject(MatDialog);
  private translate = inject(TranslateService);

  send(prompt: AiPromptMessage): void {
    this.panelService.handOff(prompt);
    this.router.navigateByUrl('/ai-assistant');
  }

  @HostListener('document:keydown', ['$event'])
  onSlash(event: KeyboardEvent): void {
    if (event.key !== '/' || event.metaKey || event.ctrlKey || event.altKey || event.defaultPrevented || this.dialog.openDialogs.length
      || (event.target as HTMLElement)?.closest?.('input, textarea, select, [contenteditable]')) {
      return;
    }
    event.preventDefault();
    this.composer?.focus();
  }
}
