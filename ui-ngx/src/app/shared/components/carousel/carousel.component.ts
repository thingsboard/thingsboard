// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterNextRender,
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  contentChild,
  contentChildren,
  DestroyRef,
  Directive,
  effect,
  ElementRef,
  forwardRef,
  inject,
  input,
  NgZone,
  signal,
  TemplateRef,
  untracked,
  viewChild,
  ViewContainerRef
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/*
 * Replacement of the `ngx-hm-carousel` library (https://github.com/ZouYouShun/ngx-hm-carousel).
 * Custom widgets still use it under that module id (modules-map.ts),
 * so names, selectors, inputs, templates and `currentIndex` repeat the library's API and the classes stay standalone.
 */
/* eslint-disable @angular-eslint/no-input-rename */

export type NgxHmCarouselAlign = 'left' | 'center' | 'right';
export type NgxHmCarouselDirection = 'left' | 'right';

export interface NgxHmCarouselDotContext {
  $implicit: {
    index: number;
    currentIndex: number;
  };
}

/** Movement after which a press becomes a drag, so a click on a slide stays a click. */
const DRAG_START_THRESHOLD = 5;
const EDGE_RESISTANCE = 0.2;
/** The release speed is measured over the last part of the gesture only. */
const VELOCITY_WINDOW_MS = 100;
const MIN_SWIPE_DISTANCE = 10;
const DEFAULT_AUTOPLAY_SPEED = 5000;
const FOCUSABLE = 'a[href], button, input, select, textarea, [tabindex], [contenteditable]';

interface DragSample {
  x: number;
  t: number;
}

@Directive({
  selector: '[ngx-hm-carousel-item]'
})
export class NgxHmCarouselItemDirective {
}

/** @deprecated Renders its content right away. */
@Directive({
  selector: '[ngxHmCarouselDynamic]'
})
export class NgxHmCarouselDynamicDirective {
  index = input<number>(undefined, { alias: 'ngxHmCarouselDynamic' });
  length = input(0, { alias: 'ngxHmCarouselDynamicLength' });
  show = input(1, { alias: 'ngxHmCarouselDynamicShow' });
  currentI = input(0, { alias: 'ngxHmCarouselDynamicIndex' });

  constructor() {
    inject(ViewContainerRef).createEmbeddedView(inject(TemplateRef));
  }
}

@Component({
  selector: 'ngx-hm-carousel',
  imports: [NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => NgxHmCarouselComponent),
      multi: true
    }
  ],
  host: {
    'role': 'region',
    'aria-roledescription': 'carousel',
    '[attr.tabindex]': 'count() > 1 ? 0 : null',
    '(keydown.arrowleft)': 'onArrowKey($event, -1)',
    '(keydown.arrowright)': 'onArrowKey($event, 1)',
    '(click)': 'onClick($event)',
    '(mouseenter)': 'onHover(true)',
    '(mouseleave)': 'onHover(false)',
    '(focusin)': 'onFocusIn($event)',
    '(focusout)': 'onFocusOut()'
  },
  templateUrl: './carousel.component.html',
  styleUrl: './carousel.component.scss'
})
export class NgxHmCarouselComponent implements ControlValueAccessor {

  private zone = inject(NgZone);
  private host: HTMLElement = inject(ElementRef).nativeElement;

  /** @deprecated No effect. */
  data = input<unknown[]>([]);
  /** @deprecated No effect. */
  aniTime = input(400);
  /** Added to the slides container when it moves to another slide; the consumer styles the transition. */
  aniClass = input('transition');
  /** Replaces `aniClass` when autoplay moves the slides. */
  aniClassAuto = input<string>(undefined);
  /** Share of the slide width a drag must cover to change the slide; `false` leaves it to the swipe speed. */
  panBoundary = input<number | false>(0.15, { alias: 'pan-boundary' });
  align = input<NgxHmCarouselAlign>('center');
  notDrag = input(false, { alias: 'not-follow-pan' });
  /** Autoplay pauses while the mouse is over the carousel. */
  mouseEnable = input(false, { alias: 'mouse-enable' });
  /** Added to `autoplay-speed` between autoplay steps, in ms. */
  delay = input(8000, { alias: 'between-delay' });
  direction = input<NgxHmCarouselDirection>('right', { alias: 'autoplay-direction' });
  scrollNum = input(1, { alias: 'scroll-num' });
  isDragMany = input(false, { alias: 'drag-many' });
  /** In px/ms. */
  swipeVelocity = input(0.3, { alias: 'swipe-velocity' });
  /** @deprecated No effect; `show-num="auto"` shows one slide. */
  breakpoint = input<unknown[]>([]);
  disableDrag = input(false, { alias: 'disable-drag' });
  infinite = input(false);
  speed = input<number>(undefined, { alias: 'autoplay-speed' });
  /** @deprecated No effect. */
  recognizers = input<unknown[]>([]);
  stopPanListener = input(false);
  showNum = input<number | 'auto'>(1, { alias: 'show-num' });
  autoplay = input(false);

  protected root = viewChild.required<ElementRef<HTMLElement>>('root');
  protected itemElms = contentChildren(NgxHmCarouselItemDirective, { read: ElementRef, descendants: true });
  protected contentPrev = contentChild('carouselPrev', { read: TemplateRef });
  protected contentNext = contentChild('carouselNext', { read: TemplateRef });
  protected contentDot = contentChild<TemplateRef<NgxHmCarouselDotContext>>('carouselDot');

  protected index = signal(0);
  protected grabbing = signal(false);
  protected count = computed(() => this.itemElms().length);
  protected dragEnabled = computed(() => !this.disableDrag() && !this.stopPanListener() && this.count() > 1);

  private shown = computed(() => {
    const showNum = this.showNum();
    return typeof showNum === 'number' && showNum >= 1 ? showNum : 1;
  });
  /** Slides between the left edge of the viewport and the selected slide. */
  private alignShift = computed(() => {
    const free = this.shown() - 1;
    return this.align() === 'left' ? 0 : this.align() === 'right' ? free : free / 2;
  });
  private loop = computed(() => this.autoplay() || this.infinite());
  private maxIndex = computed(() =>
    Math.max(0, this.align() === 'left' ? this.count() - this.shown() : this.count() - 1));

  private rendered = false;
  private animation: string = null;
  private hovered = false;
  private labelledItems = new WeakSet<HTMLElement>();
  private focused = false;
  private autoplayTimer: ReturnType<typeof setTimeout>;

  private pointerId: number = null;
  private dragging = false;
  private startX = 0;
  private startY = 0;
  private samples: DragSample[] = [];
  private suppressClick = false;

  private onChange: (index: number) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    effect(() => {
      const count = this.count();
      if (count && this.index() > count - 1) {
        untracked(() => this.select(count - 1));
      }
    });
    effect(() => {
      this.autoplay();
      this.speed();
      this.delay();
      this.count();
      untracked(() => this.restartAutoplay());
    });
    afterRenderEffect({
      write: () => this.layout()
    });
    afterNextRender(() => this.listenPointer());
    inject(DestroyRef).onDestroy(() => this.stopAutoplay());
  }

  get currentIndex(): number {
    return this.index();
  }

  set currentIndex(index: number) {
    this.goTo(index);
  }

  writeValue(value: number): void {
    if (value || value === 0) {
      this.animation = this.rendered ? this.aniClass() : null;
      this.index.set(value);
      this.restartAutoplay();
    }
  }

  registerOnChange(fn: (index: number) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  goTo(index: number, fromAutoplay = false): void {
    const count = this.count();
    if (!count) {
      return;
    }
    const target = this.loop()
      ? ((index % count) + count) % count
      : Math.min(Math.max(index, 0), this.maxIndex());
    this.animation = fromAutoplay ? (this.aniClassAuto() ?? this.aniClass()) : this.aniClass();
    if (target !== this.index()) {
      this.select(target);
    } else {
      this.move();
    }
    this.restartAutoplay();
  }

  protected step(direction: number): void {
    this.goTo(this.index() + direction * this.scrollNum());
    this.onTouched();
  }

  /** Handles the prev/next areas and the dots; keyboard users switch slides with the arrow keys. */
  protected onClick(event: Event): void {
    const root = this.root().nativeElement;
    const target = (event.target as Element).closest('.direction, .indicators > li');
    if (target?.parentElement === root) {
      this.step(target.classList.contains('left') ? -1 : 1);
    } else if (target?.matches('li') && target.parentElement.parentElement === root) {
      this.goTo(Number(target.getAttribute('data-index')));
    }
  }

  protected onArrowKey(event: Event, direction: number): void {
    if (event.target === event.currentTarget) {
      event.preventDefault();
      this.step(direction);
    }
  }

  protected onHover(hovered: boolean): void {
    this.hovered = hovered;
    if (!hovered) {
      // Focus a dialog returns to the carousel must not keep autoplay paused for a mouse user
      this.focused = false;
    }
    this.restartAutoplay();
  }

  /** Autoplay pauses while the keyboard focus is inside; a mouse click on a slide does not pause it. */
  protected onFocusIn(event: FocusEvent): void {
    this.focused = (event.target as Element).matches(':focus-visible');
    this.restartAutoplay();
  }

  protected onFocusOut(): void {
    this.focused = false;
    this.restartAutoplay();
  }

  private select(index: number): void {
    this.index.set(index);
    this.onChange(index);
  }

  private get container(): HTMLElement {
    return this.root().nativeElement.querySelector(':scope > [ngx-hm-carousel-container]');
  }

  private layout(): void {
    const container = this.container;
    const items = this.itemElms().map(item => item.nativeElement as HTMLElement);
    if (!container || !items.length) {
      return;
    }
    const count = items.length;
    const firstVisible = this.index() - this.alignShift();
    container.style.position = 'relative';
    container.style.width = `${count * 100 / this.shown()}%`;
    items.forEach((item, i) => {
      item.style.width = `${100 / count}%`;
      item.style.flexShrink = '0';
      // Keeps what the slide author set; an element with a role of its own (article, li…) may not take another one
      if (!item.hasAttribute('role') && (item.tagName === 'DIV' || item.tagName === 'SPAN')) {
        item.setAttribute('role', 'group');
      }
      if (!item.hasAttribute('aria-roledescription')) {
        item.setAttribute('aria-roledescription', 'slide');
      }
      if (!item.hasAttribute('aria-label') || this.labelledItems.has(item)) {
        item.setAttribute('aria-label', `${i + 1} / ${count}`);
        this.labelledItems.add(item);
      }
      // Off-screen slides must not take the focus
      item.inert = i <= firstVisible - 1 || i >= firstVisible + this.shown();
    });
    this.move();
    this.rendered = true;
  }

  /** Moves the slides to the selected one, shifted by `dragOffset` px while dragging. */
  private move(dragOffset = 0): void {
    const container = this.container;
    const count = this.count();
    if (!container || !count) {
      return;
    }
    container.classList.remove(...[this.aniClass(), this.aniClassAuto()].filter(Boolean));
    if (this.animation && !dragOffset) {
      container.classList.add(this.animation);
    }
    const position = -(this.index() - this.alignShift()) * 100 / count;
    container.style.transform = dragOffset
      ? `translateX(calc(${position}% + ${dragOffset}px))`
      : `translateX(${position}%)`;
  }

  private restartAutoplay(): void {
    this.stopAutoplay();
    if (!this.autoplay() || this.count() < 2 || this.dragging || this.focused || (this.hovered && this.mouseEnable())) {
      return;
    }
    const interval = (this.speed() ?? DEFAULT_AUTOPLAY_SPEED) + this.delay();
    const direction = this.direction() === 'left' ? -1 : 1;
    this.zone.runOutsideAngular(() => {
      this.autoplayTimer = setTimeout(() =>
        this.zone.run(() => this.goTo(this.index() + direction * this.scrollNum(), true)), interval);
    });
  }

  private stopAutoplay(): void {
    clearTimeout(this.autoplayTimer);
  }

  private listenPointer(): void {
    const root = this.root().nativeElement;
    this.zone.runOutsideAngular(() => {
      root.addEventListener('pointerdown', event => this.onPointerDown(event));
      root.addEventListener('pointermove', event => this.onPointerMove(event));
      root.addEventListener('pointerup', event => this.onPointerUp(event, true));
      root.addEventListener('pointercancel', event => this.onPointerUp(event, false));
      // Native image drag would take the gesture over
      root.addEventListener('dragstart', event => event.preventDefault());
      // The carousel is focusable for the keyboard only: focus taken by a click would come back
      // with a focus ring when a dialog opened from a slide (the screenshot viewer) closes
      root.addEventListener('mousedown', event => {
        if ((event.target as Element).closest(FOCUSABLE) === this.host) {
          event.preventDefault();
        }
      });
      // The click that ends a drag must not reach the slide
      root.addEventListener('click', event => {
        if (this.suppressClick) {
          this.suppressClick = false;
          event.preventDefault();
          event.stopPropagation();
        }
      }, true);
    });
  }

  private onPointerDown(event: PointerEvent): void {
    if (!this.dragEnabled() || this.pointerId !== null || !this.container?.contains(event.target as Node) ||
        (event.pointerType === 'mouse' && event.button !== 0)) {
      return;
    }
    this.pointerId = event.pointerId;
    this.startX = event.clientX;
    this.startY = event.clientY;
    this.samples = [{ x: event.clientX, t: event.timeStamp }];
  }

  private onPointerMove(event: PointerEvent): void {
    if (event.pointerId !== this.pointerId) {
      return;
    }
    let offset = event.clientX - this.startX;
    if (!this.dragging) {
      const offsetY = event.clientY - this.startY;
      if (Math.abs(offset) < DRAG_START_THRESHOLD && Math.abs(offsetY) < DRAG_START_THRESHOLD) {
        return;
      }
      if (Math.abs(offsetY) > Math.abs(offset)) {
        this.pointerId = null;
        return;
      }
      this.dragging = true;
      this.root().nativeElement.setPointerCapture(event.pointerId);
      this.stopAutoplay();
      this.grabbing.set(true);
    }
    this.samples.push({ x: event.clientX, t: event.timeStamp });
    while (this.samples.length > 2 && this.samples[1].t < event.timeStamp - VELOCITY_WINDOW_MS) {
      this.samples.shift();
    }
    const index = this.index();
    if (!this.loop() && ((offset > 0 && index <= 0) || (offset < 0 && index >= this.maxIndex()))) {
      offset *= EDGE_RESISTANCE;
    }
    if (!this.notDrag()) {
      this.move(offset);
    }
  }

  private onPointerUp(event: PointerEvent, released: boolean): void {
    if (event.pointerId !== this.pointerId) {
      return;
    }
    this.pointerId = null;
    if (!this.dragging) {
      return;
    }
    this.dragging = false;
    this.grabbing.set(false);
    const offset = event.clientX - this.startX;
    let steps = 0;
    if (released) {
      steps = this.swipeSteps(offset, { x: event.clientX, t: event.timeStamp });
      this.suppressClick = true;
      setTimeout(() => this.suppressClick = false);
    }
    this.zone.run(() => {
      this.goTo(this.index() + steps);
      this.onTouched();
    });
  }

  /** Slides to move on release: by the covered distance, otherwise by the release speed. */
  private swipeSteps(offset: number, release: DragSample): number {
    const slideWidth = this.root().nativeElement.getBoundingClientRect().width / this.shown();
    const panBoundary = this.panBoundary();
    if (panBoundary !== false && Math.abs(offset) > slideWidth * panBoundary) {
      const steps = this.isDragMany() ? Math.ceil(Math.abs(offset) / slideWidth) : this.scrollNum();
      return offset > 0 ? -steps : steps;
    }
    const first = this.samples[0];
    const duration = release.t - first.t;
    const velocity = duration > 0 ? (release.x - first.x) / duration : 0;
    if (Math.abs(velocity) > this.swipeVelocity() && Math.abs(offset) > MIN_SWIPE_DISTANCE) {
      return velocity > 0 ? -this.scrollNum() : this.scrollNum();
    }
    return 0;
  }
}
