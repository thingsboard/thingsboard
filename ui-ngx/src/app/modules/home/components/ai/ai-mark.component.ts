// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, ElementRef, inject, input, numberAttribute, booleanAttribute } from '@angular/core';

export type AiMarkCue = 'needs' | 'error';

const MARK_CYCLE_MS = 16000;
const LARGE_MARK = 44;

@Component({
  selector: 'tb-ai-mark, button[tb-ai-mark]',
  template: `
    <ng-template #glyph>
      @if (icon()) {
        <mat-icon fontSet="material-icons-round">{{ icon() }}</mat-icon>
      } @else if (large()) {
        <tb-ai-spark></tb-ai-spark>
      } @else {
        <mat-icon fontSet="material-icons-round">auto_awesome</mat-icon>
      }
    </ng-template>
    @if (button) {
      <span class="tb-ai-mark" [class.lg]="large()" [style.--m]="sizePx()" [style.animation-delay]="phase" aria-hidden="true">
        <ng-container [ngTemplateOutlet]="glyph"></ng-container>
      </span>
      @if (cue()) {
        <span class="tb-ai-mark-cue" [class]="cue()" aria-hidden="true"></span>
      }
    } @else {
      <ng-container [ngTemplateOutlet]="glyph"></ng-container>
    }
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.tb-ai-mark]': '!button',
    '[class.lg]': '!button && large()',
    '[style.--m]': 'button ? null : sizePx()',
    '[style.animation-delay]': 'button ? null : phase',
    '[attr.aria-hidden]': 'button ? null : "true"',
    '[class.tb-ai-mark-button]': 'button',
    '[attr.type]': 'button ? "button" : null',
    '[attr.aria-pressed]': 'button ? pressed() : null',
    '[class.working]': 'button && working()',
    '[class.glow]': 'button && glow()'
  }
})
export class AiMarkComponent {

  readonly button = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement.tagName === 'BUTTON';

  readonly phase = `-${Date.now() % MARK_CYCLE_MS}ms`;

  size = input<number | undefined, unknown>(undefined, {transform: (value: unknown) => value == null || value === '' ? undefined : numberAttribute(value)});

  icon = input<string>();

  pressed = input(false, {transform: booleanAttribute});

  working = input(false, {transform: booleanAttribute});

  glow = input(false, {transform: booleanAttribute});

  cue = input<AiMarkCue | null>(null);

  large(): boolean {
    return (this.size() ?? 0) >= LARGE_MARK;
  }

  sizePx(): string | null {
    const size = this.size() ?? (this.button ? 36 : undefined);
    return size ? `${size}px` : null;
  }
}
