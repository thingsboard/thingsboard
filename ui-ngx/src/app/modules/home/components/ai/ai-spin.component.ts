// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, ElementRef, inject, Input, NgZone, OnDestroy, OnInit } from '@angular/core';
import { AiSpinKind } from '@home/components/ai/ai-action.models';

const FRAME_MS = 80;
const DOT_BITS = [0x01, 0x08, 0x02, 0x10, 0x04, 0x20];

const FRAMES: Record<AiSpinKind, number[]> = {
  think: [...'⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'].map(glyph => glyph.charCodeAt(0) - 0x2800),
  listen: [0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x3f, 0x3e, 0x3c, 0x38, 0x30, 0x20, 0x00],
  gen: [0x01, 0x08, 0x10, 0x20, 0x04, 0x02].map(bit => 0x3f & ~bit),
  install: [0x09, 0x12, 0x24, 0x2d, 0x36, 0x3f, 0x3f, 0x3f, 0x00],
  stop: [0x12]
};

const spinners = new Set<AiSpinComponent>();
let tick = 0;
let timer: ReturnType<typeof setInterval> | undefined;

function startTimer(zone: NgZone): void {
  if (timer || matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return;
  }
  zone.runOutsideAngular(() => {
    timer = setInterval(() => {
      tick++;
      spinners.forEach(spinner => spinner.draw());
    }, FRAME_MS);
  });
}

@Component({
  selector: 'tb-ai-spin',
  template: '<span class="cell"><i></i><i></i><i></i><i></i><i></i><i></i></span>',
  styleUrls: ['./ai-spin.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'aria-hidden': 'true',
    '[attr.data-kind]': 'kind'
  }
})
export class AiSpinComponent implements OnInit, OnDestroy {

  private kindValue: AiSpinKind = 'think';

  @Input()
  set kind(kind: AiSpinKind) {
    this.kindValue = kind ?? 'think';
    this.draw();
  }

  get kind(): AiSpinKind {
    return this.kindValue;
  }

  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private zone = inject(NgZone);

  ngOnInit(): void {
    spinners.add(this);
    this.draw();
    startTimer(this.zone);
  }

  ngOnDestroy(): void {
    spinners.delete(this);
    if (!spinners.size && timer) {
      clearInterval(timer);
      timer = undefined;
    }
  }

  draw(): void {
    const frames = FRAMES[this.kindValue];
    const bits = frames[tick % frames.length];
    const dots = this.host.nativeElement.querySelectorAll('i');
    dots.forEach((dot, index) => dot.classList.toggle('on', !!(bits & DOT_BITS[index])));
  }
}
