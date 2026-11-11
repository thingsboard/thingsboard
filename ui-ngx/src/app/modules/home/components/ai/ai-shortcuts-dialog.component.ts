// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component } from '@angular/core';

interface ShortcutRow {
  label: string;
  keys: string[][];
}

const MOD = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform) ? '⌘' : 'Ctrl';

@Component({
  selector: 'tb-ai-shortcuts-dialog',
  template: `
    <div class="k-keys-head">
      <h2 mat-dialog-title>{{ 'ai-assistant.keys.title' | translate }}</h2>
      <button mat-icon-button type="button" class="k-keys-x" mat-dialog-close cdkFocusInitial
              [attr.aria-label]="'action.close' | translate" [matTooltip]="'action.close' | translate" matTooltipPosition="below">
        <mat-icon>close</mat-icon>
      </button>
    </div>
    <div class="k-keys-groups">
      @for (group of groups; track group.title) {
        <section>
          <h3>{{ group.title | translate }}</h3>
          @for (row of group.rows; track row.label) {
            <div class="k-keyrow">
              <span>{{ row.label | translate }}</span>
              <span class="k-keys-sets">
                @for (keys of row.keys; track $index) {
                  <span class="tip-caps">
                    @for (key of keys; track $index) {
                      <kbd>{{ key }}</kbd>
                    }
                  </span>
                }
              </span>
            </div>
          }
        </section>
      }
    </div>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'k-keys-dialog',
    'data-ai': ''
  }
})
export class AiShortcutsDialogComponent {

  readonly groups: Array<{title: string; rows: ShortcutRow[]}> = [
    {
      title: 'ai-assistant.keys.composer',
      rows: [
        {label: 'ai-assistant.send', keys: [['Enter'], [MOD, 'Enter']]},
        {label: 'ai-assistant.new-line', keys: [['Shift', 'Enter']]},
        {label: 'ai-assistant.accept-suggestion', keys: [['Tab']]},
        {label: 'ai-assistant.keys.stop', keys: [['Esc']]},
        {label: 'ai-assistant.keys.slash', keys: [['/']]}
      ]
    },
    {
      title: 'ai-assistant.keys.chat',
      rows: [
        {label: 'ai-assistant.keys.search-chats', keys: [[MOD, 'K']]},
        {label: 'ai-assistant.keys.find', keys: [[MOD, 'F']]},
        {label: 'ai-assistant.keys.close-panel', keys: [['Esc']]}
      ]
    }
  ];
}
