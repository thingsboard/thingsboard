// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component } from '@angular/core';

type Point = [number, number];

interface SparkPart {
  d: string;
  values: string;
  keyTimes: string;
  keySplines: string;
  begin: string;
}

const SPARK_WAIST = 0.55;
const CYCLE_MS = 8000;
const LOW_DELAY_MS = 2000;
const STARS: Record<'big' | 'top' | 'low', [number, number, number]> = {
  big: [9.1, 11.85, 6.8],
  top: [17.8, 5.95, 3.6],
  low: [17.8, 17.75, 3.6]
};

let sparkCount = 0;

function sparkPath(cx: number, cy: number, radius: number, waist = SPARK_WAIST, tip = 0.1): string {
  const format = (p: Point) => `${p[0].toFixed(2)} ${p[1].toFixed(2)}`;
  const inner = radius * waist;
  const tips: Point[] = [0, 1, 2, 3].map(i => {
    const angle = i * 90 * Math.PI / 180;
    return [cx + radius * Math.sin(angle), cy - radius * Math.cos(angle)];
  });
  const controls: Point[] = [0, 1, 2, 3].map(i => {
    const angle = (i * 90 + 45) * Math.PI / 180;
    const distance = 2 * inner - radius / Math.SQRT2;
    return [cx + distance * Math.sin(angle), cy - distance * Math.cos(angle)];
  });
  const along = (p0: Point, c: Point, p2: Point, t: number): Point => [
    (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c[0] + t * t * p2[0],
    (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c[1] + t * t * p2[1]
  ];
  const after: Point[] = [];
  const before: Point[] = [];
  for (let i = 0; i < 4; i++) {
    const next = (i + 1) % 4;
    after[i] = along(tips[i], controls[i], tips[next], tip);
    before[next] = along(tips[i], controls[i], tips[next], 1 - tip);
  }
  let d = 'M' + format(after[0]);
  for (let i = 0; i < 4; i++) {
    const next = (i + 1) % 4;
    d += 'Q' + format(controls[i]) + ' ' + format(before[next]) + 'Q' + format(tips[next]) + ' ' + format(after[next]);
  }
  return d + 'Z';
}

function star(kind: keyof typeof STARS, waist?: number, tip?: number): string {
  const [cx, cy, radius] = STARS[kind];
  return sparkPath(cx, cy, radius, waist, tip);
}

function part(frames: string[], begin: number): SparkPart {
  return {
    d: frames[0],
    values: frames.join(';'),
    keyTimes: frames.map((_, i) => (i / (frames.length - 1)).toFixed(3)).join(';'),
    keySplines: frames.slice(1).map(() => '0.42 0 0.58 1').join(';'),
    begin: begin + 'ms'
  };
}

@Component({
  selector: 'tb-ai-spark',
  template: `
    <svg viewBox="0 0 24 24" width="100%" height="100%" aria-hidden="true">
      <defs>
        <linearGradient [attr.id]="id" gradientUnits="userSpaceOnUse" x1="3" y1="2" x2="22" y2="22">
          <stop offset="0"/>
          <stop offset=".5"/>
          <stop offset="1"/>
        </linearGradient>
      </defs>
      @for (path of parts; track $index) {
        <path [attr.d]="path.d" [attr.fill]="fill">
          @if (!still) {
            <animate attributeName="d" dur="8000ms" repeatCount="indefinite" calcMode="spline"
                     [attr.begin]="path.begin" [attr.keyTimes]="path.keyTimes" [attr.keySplines]="path.keySplines" [attr.values]="path.values"/>
          }
        </path>
      }
    </svg>
  `,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'ai-spark-svg',
    'aria-hidden': 'true'
  }
})
export class AiSparkComponent {

  readonly id = 'tb-ai-spark-ink-' + (++sparkCount);

  readonly fill = `url(#${this.id})`;

  readonly still = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;

  readonly parts: SparkPart[];

  constructor() {
    const phase = -(Date.now() % CYCLE_MS);
    const pinch = (kind: 'top' | 'low') => [star(kind), star(kind, 0.51), star(kind), star(kind, 0.51), star(kind)];
    this.parts = [
      part([star('big'), star('big', 0.61, 0.14), star('big')], phase),
      part(pinch('top'), phase),
      part(pinch('low'), phase + LOW_DELAY_MS)
    ];
  }
}
