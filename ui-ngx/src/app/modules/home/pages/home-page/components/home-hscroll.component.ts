// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal,
  ViewChild,
  ViewEncapsulation
} from '@angular/core';

const MIN_THUMB = 32;

@Component({
  selector: 'tb-home-hscroll',
  template: `
    <div class="tb-hscroll" [class.at-start]="atStart()" [class.at-end]="atEnd()" [class.fits]="fits()" [class.dragging]="dragging()">
      <div class="tb-hscroll-view" [style.--arrow-y.px]="arrowY()">
        <div #track class="tb-hscroll-track" role="group" [attr.aria-label]="label()" (scroll)="sync()">
          <ng-content></ng-content>
        </div>
        <button type="button" class="tb-hscroll-btn prev" tabindex="-1" aria-hidden="true" (click)="step(-1)">
          <mat-icon>chevron_left</mat-icon>
        </button>
        <button type="button" class="tb-hscroll-btn next" tabindex="-1" aria-hidden="true" (click)="step(1)">
          <mat-icon>chevron_right</mat-icon>
        </button>
      </div>
      <div #bar class="tb-hscroll-bar" aria-hidden="true" (pointerdown)="press($event)">
        <i [style.width.px]="thumbWidth()" [style.transform]="'translateX(' + thumbX() + 'px)'"></i>
      </div>
    </div>
  `,
  styleUrls: ['./home-hscroll.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeHscrollComponent implements AfterViewInit {

  label = input<string>();

  @ViewChild('track', {static: true}) trackRef: ElementRef<HTMLElement>;
  @ViewChild('bar', {static: true}) barRef: ElementRef<HTMLElement>;

  readonly atStart = signal(true);
  readonly atEnd = signal(false);
  readonly fits = signal(false);
  readonly dragging = signal(false);
  readonly thumbWidth = signal(MIN_THUMB);
  readonly thumbX = signal(0);
  readonly arrowY = signal<number | null>(null);

  private destroyRef = inject(DestroyRef);

  ngAfterViewInit(): void {
    const observer = new ResizeObserver(() => this.layout());
    observer.observe(this.trackRef.nativeElement);
    const mutations = new MutationObserver(() => this.layout());
    mutations.observe(this.trackRef.nativeElement, {childList: true, subtree: true});
    this.destroyRef.onDestroy(() => {
      observer.disconnect();
      mutations.disconnect();
    });
    this.layout();
  }

  sync(): void {
    const track = this.trackRef.nativeElement;
    const width = track.clientWidth;
    const max = track.scrollWidth - width;
    const x = track.scrollLeft;
    const rail = this.barRef.nativeElement.clientWidth;
    this.atStart.set(x < 2);
    this.atEnd.set(x > max - 2);
    this.fits.set(max < 2);
    const thumb = Math.max(MIN_THUMB, rail * width / Math.max(1, track.scrollWidth));
    this.thumbWidth.set(thumb);
    this.thumbX.set(max > 0 ? (x / max) * (rail - thumb) : 0);
  }

  step(dir: number): void {
    const track = this.trackRef.nativeElement;
    const pad = parseFloat(getComputedStyle(track).paddingLeft) || 0;
    const left = track.scrollLeft;
    const right = left + track.clientWidth;
    const kids = Array.from(track.children) as HTMLElement[];
    const next = dir > 0
      ? kids.find(kid => kid.offsetLeft + kid.offsetWidth > right - pad + 1)
      : kids.reverse().find(kid => kid.offsetLeft < left + pad - 1);
    if (next) {
      track.scrollTo({left: dir > 0 ? next.offsetLeft - pad : next.offsetLeft + next.offsetWidth + pad - track.clientWidth});
    }
  }

  press(event: PointerEvent): void {
    event.preventDefault();
    const bar = this.barRef.nativeElement;
    const track = this.trackRef.nativeElement;
    bar.setPointerCapture(event.pointerId);
    this.dragging.set(true);
    const move = (moveEvent: PointerEvent) => {
      const rect = bar.getBoundingClientRect();
      const thumb = this.thumbWidth();
      const share = Math.min(1, Math.max(0, (moveEvent.clientX - rect.left - thumb / 2) / Math.max(1, rect.width - thumb)));
      track.scrollLeft = share * (track.scrollWidth - track.clientWidth);
    };
    const end = () => {
      bar.removeEventListener('pointermove', move);
      this.dragging.set(false);
    };
    bar.addEventListener('pointermove', move);
    bar.addEventListener('pointerup', end, {once: true});
    bar.addEventListener('pointercancel', end, {once: true});
    move(event);
  }

  private layout(): void {
    const view = this.trackRef.nativeElement.parentElement;
    const picture = (this.trackRef.nativeElement.querySelector('[data-hscroll-anchor]') ?? this.trackRef.nativeElement.firstElementChild) as HTMLElement;
    if (view && picture) {
      const rect = picture.getBoundingClientRect();
      if (rect.height) {
        this.arrowY.set(rect.top + rect.height / 2 - view.getBoundingClientRect().top);
      }
    }
    this.sync();
  }
}
