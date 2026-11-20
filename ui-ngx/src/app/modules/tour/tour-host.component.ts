// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, inject, Renderer2, signal, ViewContainerRef, ViewEncapsulation } from '@angular/core';

export interface TourSpot {
  left: number;
  top: number;
  width: number;
  height: number;
  radius: number;
  through: boolean;
}

@Component({
  selector: 'tb-tour-host',
  template: `
    @if (spot(); as s) {
      <div class="tb-tour-layer" [class.through]="s.through">
        <div class="tb-tour-spot" [style.left.px]="s.left" [style.top.px]="s.top" [style.width.px]="s.width" [style.height.px]="s.height"
             [style.--tb-spot-r.px]="s.radius"></div>
      </div>
    }
  `,
  styleUrls: ['./tour-host.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TourHostComponent {

  readonly viewContainerRef = inject(ViewContainerRef);

  readonly renderer = inject(Renderer2);

  readonly spot = signal<TourSpot | null>(null);
}
