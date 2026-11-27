// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AlarmSeverity } from '@shared/models/alarm.models';

const TRIANGLE = 'M6 1.3c.33 0 .64.18.8.47l4.75 8.4c.33.58-.09 1.3-.76 1.3H1.21c-.67 0-1.09-.72-.76-1.3l4.75-8.4c.16-.29.47-.47.8-.47z';

const MARKS: Record<string, string> = {
  CRITICAL: `<path class="f" d="${TRIANGLE}"/><rect class="g" x="5.3" y="4.3" width="1.4" height="3.5" rx=".7"/><circle class="g" cx="6" cy="9.35" r=".78"/>`,
  MAJOR: `<path class="f" d="${TRIANGLE}"/>`,
  MINOR: '<circle class="f" cx="6" cy="6.2" r="4.6"/>',
  WARNING: '<circle class="r" cx="6" cy="6.2" r="4.1"/>',
  INDETERMINATE: '<circle class="d" cx="6" cy="6.2" r="4.2" pathLength="16" transform="rotate(-90 6 6.2)"/>'
};

export function aiSevMarkElement(severity: string): HTMLElement {
  const mark = document.createElement('span');
  mark.className = 'tb-sev-mark ' + severity.toLowerCase();
  mark.setAttribute('aria-hidden', 'true');
  mark.innerHTML = `<svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true" focusable="false">${MARKS[severity] ?? MARKS.INDETERMINATE}</svg>`;
  return mark;
}

@Component({
  selector: 'tb-ai-sev-mark',
  template: `
    <svg viewBox="0 0 12 12" width="12" height="12" aria-hidden="true" focusable="false">
      @switch (severity()) {
        @case ('CRITICAL') {
          <path class="f" [attr.d]="triangle"/>
          <rect class="g" x="5.3" y="4.3" width="1.4" height="3.5" rx=".7"/>
          <circle class="g" cx="6" cy="9.35" r=".78"/>
        }
        @case ('MAJOR') {
          <path class="f" [attr.d]="triangle"/>
        }
        @case ('MINOR') {
          <circle class="f" cx="6" cy="6.2" r="4.6"/>
        }
        @case ('WARNING') {
          <circle class="r" cx="6" cy="6.2" r="4.1"/>
        }
        @default {
          <circle class="d" cx="6" cy="6.2" r="4.2" pathLength="16" transform="rotate(-90 6 6.2)"/>
        }
      }
    </svg>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': '"tb-sev-mark " + (severity() ?? "").toLowerCase()',
    'aria-hidden': 'true'
  }
})
export class AiSevMarkComponent {

  severity = input<AlarmSeverity | string>();

  readonly triangle = 'M6 1.3c.33 0 .64.18.8.47l4.75 8.4c.33.58-.09 1.3-.76 1.3H1.21c-.67 0-1.09-.72-.76-1.3l4.75-8.4c.16-.29.47-.47.8-.47z';
}
